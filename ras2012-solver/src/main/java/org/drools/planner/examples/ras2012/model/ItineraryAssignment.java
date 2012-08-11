package org.drools.planner.examples.ras2012.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains an {@link Itinerary} for a particular {@link Train} on a particular {@link Route}.
 * 
 */
@PlanningEntity
public final class ItineraryAssignment implements Cloneable {

    private static final Logger logger      = LoggerFactory.getLogger(ItineraryAssignment.class);

    /**
     * Numbers from 0 to this will all become wait times. This is done so that the algorithm has enough space for fine-tuning
     * the results.
     */
    private static final int    ALL_FIRST_X = 5;

    /**
     * Specifies what change there will be between two consecutive wait times. Please keep it between 0 and 1, both exclusive.
     * 
     * The current value has been carefully benchmarked against many other values and found to bring the best results.
     */
    private static final float  DECREASE_TO = 7.0f / 8.0f;

    private static List<WaitTime> getAllowedWaitTimes(final long horizon) {
        final List<WaitTime> waitTimes = new LinkedList<WaitTime>();
        int waitTime = (int) horizon;
        while (waitTime > ItineraryAssignment.ALL_FIRST_X) {
            waitTimes.add(WaitTime.getWaitTime(waitTime));
            waitTime = Math.round(waitTime * ItineraryAssignment.DECREASE_TO);
        }
        for (long i = Math.min(horizon, ItineraryAssignment.ALL_FIRST_X); i > 0; i--) {
            waitTimes.add(WaitTime.getWaitTime((int) i));
        }
        ItineraryAssignment.logger
                .info("Minutes of wait time will multiply by {}, starting with {} and until {} is reached, from where they will decrease by one.",
                        new Object[] { ItineraryAssignment.DECREASE_TO, horizon,
                                ItineraryAssignment.ALL_FIRST_X });
        ItineraryAssignment.logger.debug("Generating moves with the following wait times: "
                + waitTimes);
        return waitTimes;
    }

    private final Train                         train;

    private Route                               route;
    private Itinerary                           itinerary;
    private final Collection<MaintenanceWindow> maintenances;
    private final ProblemSolution               solution;

    /**
     * Create a new instance. Won't assign any {@link Route} to the {@link Train}.
     * 
     * @param t The train this instance should hold.
     * @param maintenances The maintenance windows from the {@link Territory}.
     */
    public ItineraryAssignment(final Train t, final ProblemSolution solution) {
        if (t == null) {
            throw new IllegalArgumentException("Train may not be null!");
        }
        this.solution = solution;
        this.train = t;
        this.maintenances = solution.getMaintenances();
    }

    /**
     * Deep-clone the object, creating a new schedule in the process. That's actually very important, otherwise score corruption
     * will occur.
     */
    @Override
    public ItineraryAssignment clone() {
        final ItineraryAssignment clone = new ItineraryAssignment(this.train, this.solution);
        clone.route = this.route;
        clone.itinerary = new Itinerary(this.route, this.train, this.maintenances);
        for (final Map.Entry<Node, WaitTime> entry : this.getItinerary().getWaitTimes().entrySet()) {
            clone.itinerary.setWaitTime(entry.getKey(), entry.getValue());
        }
        return clone;
    }

    /**
     * Retrieves the schedule for a given train and route.
     * 
     * @return The schedule.
     * @throws IllegalStateException When {@link #setRoute(Route) hasn't been called on the object.}
     */
    public Itinerary getItinerary() {
        if (this.itinerary == null) {
            throw new IllegalStateException("No itinerary available, provide a route first.");
        }
        return this.itinerary;
    }

    @PlanningVariable
    @ValueRange(type = ValueRangeType.FROM_PLANNING_ENTITY_PROPERTY, planningEntityProperty = "routes")
    public Route getRoute() {
        return this.route;
    }

    public Collection<Route> getRoutes() {
        return this.solution.getTerritory().getRoutes(this.getTrain());
    }

    public Train getTrain() {
        return this.train;
    }

    public Collection<WaitTimeAssignment> getWaitTimeAssignments() {
        if (this.route == null) {
            return Collections.emptyList();
        }
        // when train entered X minutes after start of world, don't generate wait times to cover those X minutes.
        final long horizon = this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        final Collection<WaitTimeAssignment> moves = new ArrayList<WaitTimeAssignment>();
        for (final Node waitPoint : this.getRoute().getProgression().getWaitPoints()) {
            if (!this.getItinerary().hasNode(waitPoint)) {
                continue;
            } else if (waitPoint == this.getTrain().getOrigin()
                    && waitPoint != this.getRoute().getProgression().getOrigin()
                            .getOrigin(this.getTrain())) {
                // don't allow the train to stop in cases where it would start somewhere in the middle of the territory
                continue;
            }
            final WaitTime existingWaitTime = this.getItinerary().getWaitTime(waitPoint);
            final long currentArrival = this.getItinerary().getArrivalTime(waitPoint);
            long actualHorizon = 0;
            if (currentArrival > horizon) {
                actualHorizon = existingWaitTime == null ? 0 : existingWaitTime
                        .getWaitFor(TimeUnit.MILLISECONDS);
            } else {
                // otherwise only accept wait times that won't cause the train going over the horizon much
                actualHorizon = horizon
                        - currentArrival
                        + (existingWaitTime == null ? 0 : existingWaitTime
                                .getWaitFor(TimeUnit.MILLISECONDS)) + TimeUnit.MINUTES.toMillis(60);
            }
            actualHorizon = Math.max(actualHorizon - 1, 0);
            actualHorizon = TimeUnit.MILLISECONDS.toMinutes(actualHorizon);
            for (final WaitTime wt : ItineraryAssignment.getAllowedWaitTimes(actualHorizon)) {
                if (existingWaitTime == wt) {
                    // there already is such wait time; no need to create the move
                    continue;
                }
                moves.add(new WaitTimeAssignment(waitPoint, wt));
            }
            moves.add(new WaitTimeAssignment(waitPoint, null));
        }
        return moves;
    }

    /**
     * Set a route for the train. This will in turn create a new schedule (see {@link Itinerary}).
     * 
     * @param route A route to assign the train to.
     * @throws IllegalArgumentException When route is null.
     */
    public void setRoute(final Route route) {
        if (route == null) {
            throw new IllegalArgumentException("Route may not be null.");
        }
        if (this.route != route) {
            this.route = route;
            ItineraryAssignment.logger.debug("Creating new itinerary for {}.",
                    new Object[] { this });
            this.itinerary = new Itinerary(this.route, this.train, this.maintenances);
        }
    }

    @Override
    public String toString() {
        if (this.route == null) {
            return "ItineraryAssignment [train=" + this.train.getName() + ", no route]";
        } else {
            return "ItineraryAssignment [train=" + this.train.getName() + ", route="
                    + this.route.getId() + "]";
        }
    }

}
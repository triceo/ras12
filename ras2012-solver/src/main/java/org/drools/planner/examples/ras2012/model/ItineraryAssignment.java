package org.drools.planner.examples.ras2012.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains an {@link Itinerary} for a particular {@link Train} on a particular {@link Route}.
 * 
 */
@PlanningEntity
public final class ItineraryAssignment implements Cloneable {

    private final Train                         train;

    private Route                               route;
    private Itinerary                           itinerary;
    private final Collection<MaintenanceWindow> maintenances;

    private static final Logger                 logger = LoggerFactory
                                                               .getLogger(ItineraryAssignment.class);

    /**
     * Create a new instance. Won't assign any {@link Route} to the {@link Train}, won't assign any {@link MaintenanceWindow}s.
     * 
     * @param t The train this instance should hold.
     */
    public ItineraryAssignment(final Train t) {
        this(t, Collections.<MaintenanceWindow> emptySet());
    }

    /**
     * Create a new instance. Won't assign any {@link Route} to the {@link Train}.
     * 
     * @param t The train this instance should hold.
     * @param maintenances The maintenance windows from the {@link Territory}.
     */
    public ItineraryAssignment(final Train t, final Collection<MaintenanceWindow> maintenances) {
        if (t == null) {
            throw new IllegalArgumentException("Train may not be null!");
        }
        this.train = t;
        this.maintenances = maintenances;
    }

    /**
     * Deep-clone the object, creating a new schedule in the process. That's actually very important, otherwise score corruption
     * will occur.
     */
    @Override
    public ItineraryAssignment clone() {
        final ItineraryAssignment clone = new ItineraryAssignment(this.train, this.maintenances);
        clone.route = this.route;
        clone.itinerary = new Itinerary(this.route, this.train, this.maintenances);
        for (final Map.Entry<Node, WaitTime> entry : this.getItinerary().getWaitTimes().entrySet()) {
            clone.itinerary.setWaitTime(entry.getKey(), entry.getValue());
        }
        clone.itinerary.resetLatestWaitTimeChange();
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
    @ValueRange(type = ValueRangeType.UNDEFINED)
    public Route getRoute() {
        return this.route;
    }

    public Train getTrain() {
        return this.train;
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
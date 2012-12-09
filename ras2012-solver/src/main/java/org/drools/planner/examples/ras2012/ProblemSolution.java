package org.drools.planner.examples.ras2012;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.drools.planner.api.domain.solution.PlanningEntityCollectionProperty;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.drools.planner.examples.ras2012.util.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the problem at hand. It holds all the problem entities and values.
 * 
 * There are {@link Train}s travelling on {@link Route}s in a {@link Territory}. Each train has {@link Itinerary} associated
 * with it through a {@link ItineraryAssignment}.
 * 
 * Everything except {@link ItineraryAssignment} instances and score is fixed and comes from a data set.
 * {@link ItineraryAssignment} instances change as the problem's solution changes, resulting in score changes.
 */
public class ProblemSolution extends Visualizable implements Solution<HardAndSoftScore> {

    private static final Logger                   logger      = LoggerFactory
                                                                      .getLogger(ProblemSolution.class);

    private final String                          name;

    private final Territory                       territory;
    private final Collection<MaintenanceWindow>   maintenances;
    private final Map<Train, ItineraryAssignment> assignments = new LinkedHashMap<>();
    private final SortedSet<Train>                trains;

    private HardAndSoftScore                      score;

    private long                                  horizon     = 0;

    /**
     * Create a clone of an existing solution. This clone will be exactly the same as the original solution, except for the
     * {@link ItineraryAssignment} instances, which will be deep-cloned. This is the way Planner requires it to happen.
     * 
     * @param problem The solution to clone.
     */
    private ProblemSolution(final ProblemSolution problem) {
        this.name = problem.name;
        this.territory = problem.territory;
        this.maintenances = problem.maintenances;
        this.trains = problem.trains;
        // clone assignments
        for (final ItineraryAssignment a : problem.getAssignments()) {
            this.assignments.put(a.getTrain(), a.clone());
        }
        this.horizon = problem.horizon;
        this.score = problem.getScore();
    }

    /**
     * Initialize a fresh problem. Each train will get a route assigned that is considered best for it. See
     * {@link Territory#getBestRoute(Train)}.
     * 
     * @param name Name for the problem.
     * @param trains Trains to travel on the territory.
     * @param territory Problem's territory, containing routes for trains.
     * @param maintenances Maintenance windows existing on the territory.
     */
    public ProblemSolution(final String name, final Collection<Train> trains,
            final Territory territory, final Collection<MaintenanceWindow> maintenances) {
        this.name = name;
        this.territory = territory;
        this.maintenances = maintenances;
        this.trains = new TreeSet<Train>(trains);
        /*
         * generate assignments; always pick the best route for the particular train, nevermind if it's used by another train
         * already.
         */
        // TODO separate into a solution initializer, for the sake of clarity
        for (final Train t : this.getTrains()) {
            final ItineraryAssignment ia = new ItineraryAssignment(t, this);
            ia.setRoute(this.getTerritory().getBestRoute(t));
            this.assignments.put(t, ia);
        }
    }

    @Override
    public Solution<HardAndSoftScore> cloneSolution() {
        ProblemSolution.logger.debug("Cloning solution.");
        return new ProblemSolution(this);
    }

    /**
     * Get the assignment for a particular train.
     * 
     * @param t Train in question.
     * @return The assignment for the train.
     */
    public ItineraryAssignment getAssignment(final Train t) {
        return this.assignments.get(t);
    }

    /**
     * Retrieve assignments for all the train.
     * 
     * @return The assignments.
     */
    @PlanningEntityCollectionProperty
    public Collection<ItineraryAssignment> getAssignments() {
        return this.assignments.values();
    }

    /**
     * Retrieve maintenance windows on the territory.
     * 
     * @return Maintenance windows.
     */
    public Collection<MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    /**
     * Name for the problem, usually coming from a data set.
     * 
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * The time window in which to operate. No events outside the <0, $horizon> range will count towards the score. Zero is the
     * present time.
     * 
     * @param unit Unit of time that the horizon will be returned in.
     * @return The value of the planning horizon, in the chosen unit of time.
     */
    public long getPlanningHorizon(final TimeUnit unit) {
        if (this.horizon == 0) {
            if (this.getName().endsWith("TOY")) {
                // FIXME ugly hack
                this.horizon = TimeUnit.MILLISECONDS.convert(150, TimeUnit.MINUTES);
            } else {
                this.horizon = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
            }
            if (this.horizon > Integer.MAX_VALUE) {
                throw new IllegalStateException("Your planning horizon doesn't fit into int!");
            }
        }
        return unit.convert(this.horizon, TimeUnit.MILLISECONDS);
    }

    @Override
    public Collection<? extends Object> getProblemFacts() {
        return Collections.emptyList();
    }

    @Override
    public HardAndSoftScore getScore() {
        return this.score;
    }

    /**
     * Retrieve the problem's territory.
     * 
     * @return The territory.
     */
    public Territory getTerritory() {
        return this.territory;
    }

    /**
     * Retrieve the problem's trains.
     * 
     * @return The trains.
     */
    public SortedSet<Train> getTrains() {
        return Collections.unmodifiableSortedSet(this.trains);
    }

    @Override
    public void setScore(final HardAndSoftScore score) {
        ProblemSolution.logger.debug("Setting score {} to solution {} (previous score {}).",
                new Object[] { score, this, this.score });
        this.score = score;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RAS2012Solution [name=");
        builder.append(this.name);
        builder.append(", assignments=");
        builder.append(this.assignments.values());
        builder.append(", score=");
        builder.append(this.score);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        // prepare a visualizer
        final Collection<Arc> arcs = new HashSet<>();
        for (final ItineraryAssignment ia : this.getAssignments()) {
            arcs.addAll(ia.getRoute().getProgression().getArcs());
        }
        return this.visualize(new GraphVisualizer(arcs), target);
    }

}

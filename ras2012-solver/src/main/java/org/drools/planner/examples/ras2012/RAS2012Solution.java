package org.drools.planner.examples.ras2012;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.drools.planner.api.domain.solution.PlanningEntityCollectionProperty;
import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.drools.planner.examples.ras2012.util.visualizer.GraphVisualizer;

public class RAS2012Solution extends Visualizable implements Solution<HardAndSoftScore> {

    private final String                          name;

    private final Territory                       territory;
    private final Collection<MaintenanceWindow>   maintenances;
    private final Map<Train, ItineraryAssignment> assignments = new LinkedHashMap<Train, ItineraryAssignment>();
    private final Collection<Train>               trains;

    private HardAndSoftScore                      score;

    private long                                  horizon     = 0;

    public RAS2012Solution(final String name, final Territory territory,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains) {
        this.name = name;
        this.territory = territory;
        this.maintenances = maintenances;
        this.trains = trains;
        /*
         * generate assignments; always pick the best route for the particular train, nevermind if it's used by another train
         * already.
         */
        // TODO separate into a solution initializer, for the sake of clarity
        for (final Train t : this.getTrains()) {
            final ItineraryAssignment ia = new ItineraryAssignment(t, maintenances);
            ia.setRoute(this.getTerritory().getBestRoute(t));
            this.assignments.put(t, ia);
        }
    }

    private RAS2012Solution(final String name, final Territory territory,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains,
            final Collection<ItineraryAssignment> assignments) {
        this.name = name;
        this.territory = territory;
        this.maintenances = maintenances;
        this.trains = trains;
        // clone assignments
        for (final ItineraryAssignment a : assignments) {
            this.assignments.put(a.getTrain(), a.clone());
        }
    }

    @Override
    public Solution<HardAndSoftScore> cloneSolution() {
        final RAS2012Solution solution = new RAS2012Solution(this.name, this.territory,
                this.maintenances, this.trains, this.getAssignments());
        solution.horizon = this.horizon;
        solution.score = (this.score == null) ? null : DefaultHardAndSoftScore.valueOf(
                this.score.getHardScore(), this.score.getSoftScore());
        return solution;
    }

    public ItineraryAssignment getAssignment(final Train t) {
        return this.assignments.get(t);
    }

    @PlanningEntityCollectionProperty
    public Collection<ItineraryAssignment> getAssignments() {
        return this.assignments.values();
    }

    public Collection<MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    public String getName() {
        return this.name;
    }

    public Territory getTerritory() {
        return this.territory;
    }

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
        final Collection<Object> allFacts = new LinkedList<Object>();
        return allFacts;
    }

    @Override
    public HardAndSoftScore getScore() {
        return this.score;
    }

    public SortedSet<Train> getTrains() {
        return new TreeSet<Train>(this.trains);
    }

    @Override
    public void setScore(final HardAndSoftScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RAS2012Solution [name=");
        builder.append(this.name);
        builder.append(", assignments=");
        builder.append(this.assignments);
        builder.append(", score=");
        builder.append(this.score);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        final Collection<Arc> arcs = new HashSet<Arc>();
        for (final ItineraryAssignment ia : this.getAssignments()) {
            arcs.addAll(ia.getRoute().getProgression().getArcs());
        }
        return this.visualize(new GraphVisualizer(arcs), target);
    }

}

package org.drools.planner.examples.ras2012;

import java.util.Collection;

import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Train;

public class RAS2012Solution implements Solution<HardAndSoftScore> {

    private final String                        name;
    private final Collection<Arc>               arcs;
    private final Collection<MaintenanceWindow> maintenances;
    private final Collection<Train>             trains;

    private HardAndSoftScore                    score;

    public RAS2012Solution(final String name, final Collection<Arc> arcs,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains) {
        this.name = name;
        this.arcs = arcs;
        this.maintenances = maintenances;
        this.trains = trains;
    }

    @Override
    public Solution<HardAndSoftScore> cloneSolution() {
        return new RAS2012Solution(this.name, this.arcs, this.maintenances, this.trains);
    }

    public Collection<Arc> getArcs() {
        return this.arcs;
    }

    public Collection<MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Collection<? extends Object> getProblemFacts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HardAndSoftScore getScore() {
        return this.score;
    }

    public Collection<Train> getTrains() {
        return this.trains;
    }

    @Override
    public void setScore(final HardAndSoftScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "RAS2012Solution [name=" + this.name + ", arcs=" + this.arcs + ", maintenances="
                + this.maintenances + ", trains=" + this.trains + ", score=" + this.score + "]";
    }

}

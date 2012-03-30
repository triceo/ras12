package org.drools.planner.examples.ras2012;

import java.util.Collection;

import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Network;
import org.drools.planner.examples.ras2012.model.Train;

public class RAS2012Solution implements Solution<HardAndSoftScore> {

    public static final Integer                 PLANNING_HORIZON_MINUTES = 12 * 60;

    private final String                        name;
    private final Network                       net;
    private final Collection<MaintenanceWindow> maintenances;
    private final Collection<Train>             trains;

    private HardAndSoftScore                    score;

    public RAS2012Solution(final String name, Network net,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains) {
        this.name = name;
        this.net = net;
        this.maintenances = maintenances;
        this.trains = trains;
    }

    @Override
    public Solution<HardAndSoftScore> cloneSolution() {
        return new RAS2012Solution(this.name, this.net, this.maintenances, this.trains);
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
        StringBuilder builder = new StringBuilder();
        builder.append("RAS2012Solution [name=").append(name).append(", net=").append(net)
                .append(", maintenances=").append(maintenances).append(", trains=").append(trains)
                .append(", score=").append(score).append("]");
        return builder.toString();
    }

}

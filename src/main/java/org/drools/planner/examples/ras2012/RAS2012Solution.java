package org.drools.planner.examples.ras2012;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Network;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

public class RAS2012Solution implements Solution<HardAndSoftScore> {

    public static final Integer                 PLANNING_HORIZON_MINUTES = 12 * 60;

    private final String                        name;
    private final Network                       net;
    private final Collection<MaintenanceWindow> maintenances;
    private final Collection<Train>             trains;
    private Map<Train, Set<Itinerary>>          itineraries              = new HashMap<Train, Set<Itinerary>>();

    private HardAndSoftScore                    score;

    public RAS2012Solution(final String name, final Network net,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains) {
        this.name = name;
        this.net = net;
        this.maintenances = maintenances;
        this.trains = trains;
        for (final Train t : this.trains) {
            final Collection<Route> routes = t.isEastbound() ? this.net.getAllEastboundRoutes()
                    : this.net.getAllWestboundRoutes();
            for (final Route r : routes) {
                if (!r.isPossibleForTrain(t)) {
                    continue;
                }
                if (!this.itineraries.containsKey(t)) {
                    this.itineraries.put(t, new HashSet<Itinerary>());
                }
                this.itineraries.get(t).add(new Itinerary(r, t, maintenances));
            }
        }
    }

    private RAS2012Solution(final String name, final Network net,
            final Collection<MaintenanceWindow> maintenances, final Collection<Train> trains,
            final Map<Train, Set<Itinerary>> itineraries) {
        this.name = name;
        this.net = net;
        this.maintenances = maintenances;
        this.trains = trains;
        this.itineraries = itineraries;
    }

    @Override
    public Solution<HardAndSoftScore> cloneSolution() {
        return new RAS2012Solution(this.name, this.net, this.maintenances, this.trains,
                this.itineraries);
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
        final StringBuilder builder = new StringBuilder();
        builder.append("RAS2012Solution [name=").append(this.name).append(", net=")
                .append(this.net).append(", maintenances=").append(this.maintenances)
                .append(", trains=").append(this.trains).append(", score=").append(this.score)
                .append("]");
        return builder.toString();
    }

}

package org.drools.planner.examples.ras2012.model.planner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

@PlanningEntity
public class ItineraryAssignment {

    @Override
    public String toString() {
        return "ItineraryAssignment [train=" + train + ", route=" + route + "]";
    }

    private Train                         train;
    private Route                         route;
    private Itinerary                     itinerary;
    private Collection<MaintenanceWindow> maintenances;

    public ItineraryAssignment(Train t, Collection<MaintenanceWindow> maintenances) {
        this.train = t;
        this.maintenances = maintenances;
    }

    public Train getTrain() {
        return train;
    }

    @PlanningVariable
    @ValueRange(type = ValueRangeType.UNDEFINED)
    public Route getRoute() {
        return route;
    }

    private final Map<Route, Itinerary> itineraries = new HashMap<Route, Itinerary>();

    public synchronized void setRoute(Route route) {
        if (!route.isPossibleForTrain(this.train)) {
            throw new IllegalArgumentException(route + " not possible for " + train);
        }
        this.route = route;
        if (!itineraries.containsKey(route)) {
            this.itineraries.put(route, new Itinerary(this.route, this.train, this.maintenances));
        }
        this.itinerary = this.itineraries.get(route);
        this.itinerary.removeAllWaitTimes();
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public ItineraryAssignment clone() {
        ItineraryAssignment ia = new ItineraryAssignment(this.train, this.maintenances);
        ia.route = this.route;
        ia.itinerary = this.itinerary;
        return ia;
    }

}
package org.drools.planner.examples.ras2012.model.planner;

import java.util.Collection;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryInterface;
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
    private ItineraryInterface            itinerary;
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

    public synchronized void setRoute(Route route) {
        if (!route.isPossibleForTrain(this.train)) {
            throw new IllegalArgumentException(route + " not possible for " + train);
        }
        this.route = route;
        this.itinerary = new Itinerary(this.route, this.train, this.maintenances);
    }

    public ItineraryInterface getItinerary() {
        return itinerary;
    }

    public ItineraryAssignment clone() {
        ItineraryAssignment ia = new ItineraryAssignment(this.train, this.maintenances);
        ia.route = this.route;
        ia.itinerary = this.itinerary;
        return ia;
    }

}
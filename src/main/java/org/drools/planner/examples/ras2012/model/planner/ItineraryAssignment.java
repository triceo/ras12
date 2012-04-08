package org.drools.planner.examples.ras2012.model.planner;

import java.util.Collection;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

@PlanningEntity
public class ItineraryAssignment {

    private final Train                         train;

    private Route                               route;
    private ScheduleProducer                    itinerary;
    private final Collection<MaintenanceWindow> maintenances;

    public ItineraryAssignment(final Train t, final Collection<MaintenanceWindow> maintenances) {
        this.train = t;
        this.maintenances = maintenances;
    }

    @Override
    public ItineraryAssignment clone() {
        final ItineraryAssignment ia = new ItineraryAssignment(this.train, this.maintenances);
        ia.route = this.route;
        ia.itinerary = this.itinerary;
        return ia;
    }

    public ScheduleProducer getItinerary() {
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

    public synchronized void setRoute(final Route route) {
        if (!route.isPossibleForTrain(this.train)) {
            throw new IllegalArgumentException(route + " not possible for " + this.train);
        }
        this.route = route;
        this.itinerary = new Itinerary(this.route, this.train, this.maintenances);
    }

    @Override
    public String toString() {
        return "ItineraryAssignment [train=" + this.train + ", route=" + this.route + "]";
    }

}
package org.drools.planner.examples.ras2012.model;

import java.util.Collection;

import org.drools.planner.api.domain.entity.PlanningEntity;
import org.drools.planner.api.domain.variable.PlanningVariable;
import org.drools.planner.api.domain.variable.ValueRange;
import org.drools.planner.api.domain.variable.ValueRangeType;
import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;

@PlanningEntity
public final class ItineraryAssignment implements Cloneable {

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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ItineraryAssignment)) {
            return false;
        }
        final ItineraryAssignment other = (ItineraryAssignment) obj;
        if (this.itinerary == null) {
            if (other.itinerary != null) {
                return false;
            }
        } else if (!this.itinerary.equals(other.itinerary)) {
            return false;
        }
        return true;
    }

    public ScheduleProducer getItinerary() {
        return this.itinerary;
    }

    @PlanningVariable
    @ValueRange(type = ValueRangeType.UNDEFINED)
    public synchronized Route getRoute() {
        return this.route;
    }

    public Train getTrain() {
        return this.train;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.itinerary == null ? 0 : this.itinerary.hashCode());
        return result;
    }

    public synchronized void setRoute(final Route route) {
        this.route = route;
        this.itinerary = new Itinerary(this.route, this.train, this.maintenances);
    }

    @Override
    public String toString() {
        return "ItineraryAssignment [train=" + this.train.getName() + ", route="
                + this.route.getId() + "]";
    }

}
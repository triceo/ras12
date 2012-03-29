package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;

public class Route {

    public static enum Direction {
        EASTBOUND, WESTBOUND
    }

    private final Collection<Arc> parts = new LinkedList<Arc>();
    private final Direction       direction;

    public Route(final Direction d) {
        this.direction = d;
    }

    public Route(final Direction d, final Arc... e) {
        this(d);
        for (final Arc a : e) {
            this.parts.add(a);
        }
    }

    public boolean contains(final Arc e) {
        return this.parts.contains(e);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Route other = (Route) obj;
        if (this.direction != other.direction) {
            return false;
        }
        if (this.parts == null) {
            if (other.parts != null) {
                return false;
            }
        } else if (!this.parts.equals(other.parts)) {
            return false;
        }
        return true;
    }

    public Route extend(final Arc e) {
        final Collection<Arc> newParts = new ArrayList<Arc>(this.parts);
        newParts.add(e);
        final Arc[] result = newParts.toArray(new Arc[newParts.size()]);
        return new Route(this.getDirection(), result);
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BigDecimal getLengthInMiles() {
        BigDecimal result = BigDecimal.ZERO;
        for (final Arc a : this.parts) {
            result = result.add(a.getLengthInMiles());
        }
        return result;
    }

    public int getLengthInNodes() {
        return this.parts.size();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.direction == null ? 0 : this.direction.hashCode());
        result = prime * result + (this.parts == null ? 0 : this.parts.hashCode());
        return result;
    }

    public boolean isPossibleForTrain(final Train t) {
        // make sure both the route and the train are in the same direction
        final boolean bothEastbound = t.isEastbound() && this.getDirection() == Direction.EASTBOUND;
        final boolean bothWestbound = t.isWestbound() && this.getDirection() == Direction.WESTBOUND;
        if (!bothEastbound && !bothWestbound) {
            return false;
        }
        // make sure the route doesn't contain a siding shorter than the train
        for (final Arc a : this.parts) {
            if (a.getTrackType() == TrackType.SIDING) {
                final int result = a.getLengthInMiles().compareTo(t.getLength());
                if (result < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Route [direction=").append(this.direction).append(", parts=")
                .append(this.parts).append("]");
        return builder.toString();
    }

}

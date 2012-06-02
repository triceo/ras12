package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ScheduleAdherenceRequirement {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final Node            destination;

    private final long            timeSinceStartOfWorld;

    public ScheduleAdherenceRequirement(final Node where, final int when) {
        if (where == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }
        this.destination = where;
        this.timeSinceStartOfWorld = ScheduleAdherenceRequirement.DEFAULT_TIME_UNIT.convert(when,
                TimeUnit.MINUTES);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ScheduleAdherenceRequirement)) {
            return false;
        }
        final ScheduleAdherenceRequirement other = (ScheduleAdherenceRequirement) obj;
        if (this.destination != other.destination) {
            return false;
        }
        if (this.timeSinceStartOfWorld != other.timeSinceStartOfWorld) {
            return false;
        }
        return true;
    }

    public Node getDestination() {
        return this.destination;
    }

    public long getTimeSinceStartOfWorld(final TimeUnit unit) {
        return unit.convert(this.timeSinceStartOfWorld,
                ScheduleAdherenceRequirement.DEFAULT_TIME_UNIT);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getDestination())
                .append(this.getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS)).build();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ScheduleAdherenceRequirement [destination=").append(this.destination)
                .append(", timeSinceStartOfWorld=").append(this.timeSinceStartOfWorld).append("]");
        return builder.toString();
    }

}

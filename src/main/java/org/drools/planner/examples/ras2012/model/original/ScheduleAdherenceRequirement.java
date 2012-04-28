package org.drools.planner.examples.ras2012.model.original;

import java.util.concurrent.TimeUnit;

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
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.destination == null ? 0 : this.destination.hashCode());
        result = prime * result
                + (int) (this.timeSinceStartOfWorld ^ this.timeSinceStartOfWorld >>> 32);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ScheduleAdherenceRequirement [destination=").append(this.destination)
                .append(", timeSinceStartOfWorld=").append(this.timeSinceStartOfWorld).append("]");
        return builder.toString();
    }

}

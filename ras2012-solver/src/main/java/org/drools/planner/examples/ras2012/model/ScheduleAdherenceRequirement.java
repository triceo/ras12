package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Schedule adherence requirement specifies that a {@link Train} is expected to arrive at a given {@link Node} at a given time.
 * 
 */
public class ScheduleAdherenceRequirement {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final Node            destination;

    private final long            timeSinceStartOfWorld;

    /**
     * Creates a new instance.
     * 
     * @param where The node where the {@link Train} is expected.
     * @param when The time that it should be there at, in minutes since the beginning of planning horizon.
     */
    public ScheduleAdherenceRequirement(final Node where, final int when) {
        if (where == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }
        this.destination = where;
        this.timeSinceStartOfWorld = ScheduleAdherenceRequirement.DEFAULT_TIME_UNIT.convert(when,
                TimeUnit.MINUTES);
    }

    /**
     * Two instances equal if both the {@link Node} and time equal.
     * 
     * @param obj The other instance.
     */
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
        return new EqualsBuilder()
                .append(this.getDestination(), other.getDestination())
                .append(this.getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS),
                        other.getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS)).isEquals();
    }

    public Node getDestination() {
        return this.destination;
    }

    /**
     * Get the expected time to arrive at {@link #getDestination()}.
     * 
     * @param unit The unit of time to return the time in.
     * @return The time in a given unit.
     */
    public long getTimeSinceStartOfWorld(final TimeUnit unit) {
        return unit.convert(this.timeSinceStartOfWorld,
                ScheduleAdherenceRequirement.DEFAULT_TIME_UNIT);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getDestination())
                .append(this.getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS)).toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ScheduleAdherenceRequirement [destination=").append(this.destination)
                .append(", timeSinceStartOfWorld=").append(this.timeSinceStartOfWorld).append("]");
        return builder.toString();
    }

}

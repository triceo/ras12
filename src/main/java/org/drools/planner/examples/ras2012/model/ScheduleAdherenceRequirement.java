package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleAdherenceRequirement {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final Integer              id          = ScheduleAdherenceRequirement.idGenerator
                                                           .incrementAndGet();

    private final Node                 destination;

    private final int                  timeSinceStartOfWorld;

    public ScheduleAdherenceRequirement(final Node where, final int when) {
        this.destination = where;
        this.timeSinceStartOfWorld = when;
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
        final ScheduleAdherenceRequirement other = (ScheduleAdherenceRequirement) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public Node getDestination() {
        return this.destination;
    }

    public int getTimeSinceStartOfWorld() {
        return this.timeSinceStartOfWorld;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.id == null ? 0 : this.id.hashCode());
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

package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.atomic.AtomicInteger;

public class MaintenanceWindow {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final Integer              id          = MaintenanceWindow.idGenerator
                                                           .incrementAndGet();

    private final Node                 startNode;
    private final Node                 endNode;
    private final int                  startingMinute;
    private final int                  endingMinute;

    // FIXME just in case; make sure start/end are in ascending order
    public MaintenanceWindow(final Node startNode, final Node endNode, final int startingMinute,
            final int endingMinute) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.startingMinute = startingMinute;
        this.endingMinute = endingMinute;
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
        final MaintenanceWindow other = (MaintenanceWindow) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public int getEndingMinute() {
        return this.endingMinute;
    }

    public Node getEndNode() {
        return this.endNode;
    }

    public int getStartingMinute() {
        return this.startingMinute;
    }

    public Node getStartNode() {
        return this.startNode;
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
        builder.append("MaintenanceWindow [startNode=").append(this.startNode).append(", endNode=")
                .append(this.endNode).append(", startingMinute=").append(this.startingMinute)
                .append(", endingMinute=").append(this.endingMinute).append("]");
        return builder.toString();
    }

}

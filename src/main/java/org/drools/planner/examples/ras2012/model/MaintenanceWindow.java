package org.drools.planner.examples.ras2012.model;

public class MaintenanceWindow {

    private final Node startNode;
    private final Node endNode;
    private final int  startingMinute;
    private final int  endingMinute;

    // FIXME just in case; make sure start/end are in ascending order
    public MaintenanceWindow(final Node startNode, final Node endNode, final int startingMinute,
            final int endingMinute) {
        if (startNode == null || endNode == null) {
            throw new IllegalArgumentException("Neither node can be null.");
        }
        if (startingMinute < 0 || endingMinute < 0) {
            throw new IllegalArgumentException("Neither time can be less than zero.");
        }
        if (startingMinute >= endingMinute) {
            throw new IllegalArgumentException("Maintenance must end after it started.");
        }
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
        if (this.endNode == null) {
            if (other.endNode != null) {
                return false;
            }
        } else if (!this.endNode.equals(other.endNode)) {
            return false;
        }
        if (this.endingMinute != other.endingMinute) {
            return false;
        }
        if (this.startNode == null) {
            if (other.startNode != null) {
                return false;
            }
        } else if (!this.startNode.equals(other.startNode)) {
            return false;
        }
        if (this.startingMinute != other.startingMinute) {
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
        result = prime * result + (this.endNode == null ? 0 : this.endNode.hashCode());
        result = prime * result + this.endingMinute;
        result = prime * result + (this.startNode == null ? 0 : this.startNode.hashCode());
        result = prime * result + this.startingMinute;
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

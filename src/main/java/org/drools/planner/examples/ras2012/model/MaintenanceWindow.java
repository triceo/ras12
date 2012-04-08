package org.drools.planner.examples.ras2012.model;

public class MaintenanceWindow {

    private final Node westNode;
    private final Node eastNode;
    private final int  startingMinute;
    private final int  endingMinute;

    public MaintenanceWindow(final Node westNode, final Node eastNode, final int time1,
            final int time2) {
        if (eastNode == null || westNode == null) {
            throw new IllegalArgumentException("Neither node can be null.");
        }
        if (time1 < 0 || time2 < 0) {
            throw new IllegalArgumentException("Neither time can be less than zero.");
        }
        this.westNode = westNode;
        this.eastNode = eastNode;
        this.startingMinute = Math.min(time1, time2);
        this.endingMinute = Math.max(time1, time2);
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
        if (this.eastNode == null) {
            if (other.eastNode != null) {
                return false;
            }
        } else if (!this.eastNode.equals(other.eastNode)) {
            return false;
        }
        if (this.endingMinute != other.endingMinute) {
            return false;
        }
        if (this.westNode == null) {
            if (other.westNode != null) {
                return false;
            }
        } else if (!this.westNode.equals(other.westNode)) {
            return false;
        }
        if (this.startingMinute != other.startingMinute) {
            return false;
        }
        return true;
    }

    public Node getEastNode() {
        return this.eastNode;
    }

    public int getEndingMinute() {
        return this.endingMinute;
    }

    public int getStartingMinute() {
        return this.startingMinute;
    }

    public Node getWestNode() {
        return this.westNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.eastNode == null ? 0 : this.eastNode.hashCode());
        result = prime * result + this.endingMinute;
        result = prime * result + (this.westNode == null ? 0 : this.westNode.hashCode());
        result = prime * result + this.startingMinute;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MaintenanceWindow [startNode=").append(this.westNode).append(", endNode=")
                .append(this.eastNode).append(", startingMinute=").append(this.startingMinute)
                .append(", endingMinute=").append(this.endingMinute).append("]");
        return builder.toString();
    }

}

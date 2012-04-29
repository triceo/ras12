package org.drools.planner.examples.ras2012.model.original;

import org.drools.planner.examples.ras2012.interfaces.Directed;

public abstract class Section {

    private final Node westNode, eastNode;

    protected Section(final Node westNode, final Node eastNode) {
        if (eastNode == null || westNode == null) {
            throw new IllegalArgumentException("Neither node can be null.");
        }
        if (westNode == eastNode) {
            throw new IllegalArgumentException("Sections must be between two different nodes.");
        }
        this.westNode = westNode;
        this.eastNode = eastNode;
    }

    public Node getDestination(final Directed d) {
        if (d.isEastbound()) {
            return this.eastNode;
        } else {
            return this.westNode;
        }
    }

    public Node getEastNode() {
        return this.eastNode;
    }

    public Node getOrigin(final Directed d) {
        if (d.isEastbound()) {
            return this.westNode;
        } else {
            return this.eastNode;
        }
    }

    public Node getWestNode() {
        return this.westNode;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(this.westNode.getId()).append("->")
                .append(this.eastNode.getId()).append("]");
        return builder.toString();
    }

}

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

    public Node getEastNode() {
        return this.eastNode;
    }

    public Node getInitialNode(final Directed d) {
        if (d.isEastbound()) {
            return this.eastNode;
        } else {
            return this.westNode;
        }
    }

    public Node getTerminalNode(final Directed d) {
        if (d.isEastbound()) {
            return this.westNode;
        } else {
            return this.eastNode;
        }
    }

    public Node getWestNode() {
        return this.westNode;
    }

}

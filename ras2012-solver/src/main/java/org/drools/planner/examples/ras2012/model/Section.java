package org.drools.planner.examples.ras2012.model;

import org.drools.planner.examples.ras2012.Directed;

/**
 * A section is part of {@link Route} that is delimited by two {@link Node}s.
 */
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

    /**
     * The node that ends the section, based on the direction.
     * 
     * @param d The direction.
     * @return The ending node.
     */
    public Node getDestination(final Directed d) {
        if (d.isEastbound()) {
            return this.eastNode;
        } else {
            return this.westNode;
        }
    }

    protected Node getEastNode() {
        return this.eastNode;
    }

    /**
     * The node that starts the section, based on the direction.
     * 
     * @param d The direction.
     * @return The starting node.
     */
    public Node getOrigin(final Directed d) {
        if (d.isEastbound()) {
            return this.westNode;
        } else {
            return this.eastNode;
        }
    }

    protected Node getWestNode() {
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

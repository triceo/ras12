package org.drools.planner.examples.ras2012.model;

/**
 * Represents a relationship between a wait point and a wait time.
 */
public class WaitTimeAssignment {

    private final Node     node;
    private final WaitTime waitTime;

    public WaitTimeAssignment(final Node n, final WaitTime wt) {
        this.node = n;
        this.waitTime = wt;
    }

    public Node getNode() {
        return this.node;
    }

    public WaitTime getWaitTime() {
        return this.waitTime;
    }

}

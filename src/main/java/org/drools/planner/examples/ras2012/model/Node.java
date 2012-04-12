package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;

public class Node implements Comparable<Node> {

    private final int                       id;
    private final String                    asString;

    private static final Map<Integer, Node> nodes = new HashMap<Integer, Node>();

    public static synchronized Node getNode(final int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Node ID cannot be less than zero!");
        }
        if (!Node.nodes.containsKey(id)) {
            final Node n = new Node(id);
            Node.nodes.put(id, n);
            return n;
        }
        return Node.nodes.get(id);

    }

    private Node(final int id) {
        this.id = id;
        this.asString = this.toStringInternal();
    }

    @Override
    public int compareTo(final Node arg0) {
        if (this.getId() > arg0.getId()) {
            return 1;
        } else if (this.getId() == arg0.getId()) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Node)) {
            return false;
        }
        final Node other = (Node) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        return result;
    }

    @Override
    public String toString() {
        return this.asString;
    }

    private String toStringInternal() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Node #").append(this.id);
        return builder.toString();
    }

}

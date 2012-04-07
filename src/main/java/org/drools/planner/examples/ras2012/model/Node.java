package org.drools.planner.examples.ras2012.model;

public class Node implements Comparable<Node> {

    private final int    id;
    private final String asString;

    public Node(final int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Node ID cannot be less than zero!");
        }
        this.id = id;
        this.asString = this.toStringInternal();
    }

    @Override
    public int compareTo(final Node arg0) {
        return Integer.valueOf(this.getId()).compareTo(arg0.getId());
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

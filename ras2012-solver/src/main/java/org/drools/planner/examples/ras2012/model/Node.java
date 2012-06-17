package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class Node implements Comparable<Node> {

    private final int                       id;

    private static final Map<Integer, Node> nodes = new HashMap<Integer, Node>();

    public static int count() {
        return Node.nodes.size();
    }

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
    }

    @Override
    public int compareTo(final Node arg0) {
        return new CompareToBuilder().append(this.getId(), arg0.getId()).toComparison();
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "Node #" + this.getId();
    }

}

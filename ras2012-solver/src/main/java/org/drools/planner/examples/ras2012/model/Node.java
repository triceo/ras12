package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * A node is a place on the {@link Route} where two {@link Arc}s join or where the {@link Route} ends.
 */
public class Node implements Comparable<Node> {

    private final int                       id;

    private static final Map<Integer, Node> nodes = new HashMap<Integer, Node>();

    /**
     * Get the number of registered nodes.
     * 
     * @return Number of nodes.
     */
    public static int count() {
        return Node.nodes.size();
    }

    /**
     * A node is uniquely identified by its numeric ID.
     * 
     * @param id The unique ID of the node.
     * @return The node in question.
     */
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

    /**
     * Creating nodes from the application code isn't allowed. See {@link #getNode(int)}.
     * 
     * @param id The ID of the node.
     */
    private Node(final int id) {
        this.id = id;
    }

    /**
     * Node with a greater ID is considered greater.
     */
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

package org.drools.planner.examples.ras2012.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.drools.planner.examples.ras2012.model.Route.Direction;

/**
 * Assumptions: node #0 is WEST-most node.
 * 
 */
public class Network {

    private final Map<Integer, Node>        nodes = new HashMap<Integer, Node>();
    private final Node                      eastDepo;
    private final Node                      westDepo;
    private final Map<Node, Map<Node, Arc>> eastboundConnections;
    private final Map<Node, Map<Node, Arc>> westboundConnections;
    private Collection<Route>               westboundRoutes;
    private Collection<Route>               eastboundRoutes;

    public Network(final Collection<Node> nodes, final Collection<Arc> edges) {
        for (final Node n : nodes) {
            this.nodes.put(n.getId(), n);
        }
        // now map every connection node
        final Map<Node, Map<Node, Arc>> tmpEastboundConnections = new HashMap<Node, Map<Node, Arc>>();
        final Map<Node, Map<Node, Arc>> tmpWestboundConnections = new HashMap<Node, Map<Node, Arc>>();
        for (final Arc a : edges) {
            if (tmpEastboundConnections.get(a.getStartingNode()) == null) {
                tmpEastboundConnections.put(a.getStartingNode(), new HashMap<Node, Arc>());
            }
            tmpEastboundConnections.get(a.getStartingNode()).put(a.getEndingNode(), a);
            if (tmpWestboundConnections.get(a.getEndingNode()) == null) {
                tmpWestboundConnections.put(a.getEndingNode(), new HashMap<Node, Arc>());
            }
            tmpWestboundConnections.get(a.getEndingNode()).put(a.getStartingNode(), a);
        }
        this.eastboundConnections = Collections.unmodifiableMap(tmpEastboundConnections);
        this.westboundConnections = Collections.unmodifiableMap(tmpWestboundConnections);
        this.eastDepo = this.locateEastboundDepo();
        this.westDepo = this.locateWestboundDepo();
    }

    public synchronized Collection<Route> getAllEastboundRoutes() {
        if (this.eastboundRoutes == null) {
            this.eastboundRoutes = this.getAllRoutes(new Route(Direction.EASTBOUND),
                    this.eastboundConnections, this.westDepo);
        }
        return this.eastboundRoutes;
    }

    private Collection<Route> getAllRoutes(final Route r,
            final Map<Node, Map<Node, Arc>> connections, final Node startingNode) {
        final Collection<Route> routes = new HashSet<Route>();
        if (connections.get(startingNode) == null) {
            return routes;
        }
        for (final Map.Entry<Node, Arc> e : connections.get(startingNode).entrySet()) {
            final Node nextNode = e.getKey();
            final Arc edge = e.getValue();
            if (r.contains(edge)) {
                continue; // we'we been there already; skip this branch
            }
            final Route newRoute = r.extend(edge);
            final Collection<Route> newRoutes = this.getAllRoutes(newRoute, connections, nextNode);
            if (newRoutes.size() > 0) {
                routes.addAll(newRoutes);
            } else {
                routes.add(newRoute);
            }
        }
        return routes;
    }

    public synchronized Collection<Route> getAllWestboundRoutes() {
        if (this.westboundRoutes == null) {
            this.westboundRoutes = this.getAllRoutes(new Route(Direction.WESTBOUND),
                    this.westboundConnections, this.eastDepo);
        }
        return this.westboundRoutes;
    }

    private Node locateEastboundDepo() {
        for (final Node n : this.nodes.values()) {
            if (!this.eastboundConnections.containsKey(n)) {
                return n;
            }
        }
        return null;
    }

    private Node locateWestboundDepo() {
        for (final Node n : this.nodes.values()) {
            if (!this.westboundConnections.containsKey(n)) {
                return n;
            }
        }
        return null;
    }
}

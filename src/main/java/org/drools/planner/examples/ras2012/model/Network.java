package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.interfaces.Visualizable;
import org.drools.planner.examples.ras2012.model.Route.Direction;
import org.drools.planner.examples.ras2012.util.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assumptions: node #0 is WEST-most node.
 * 
 */
public class Network implements Visualizable {

    private static final Logger                         logger = LoggerFactory
                                                                       .getLogger(Network.class);

    private final GraphVisualizer                       visualizer;
    private final SortedMap<Integer, Node>              nodes  = new TreeMap<Integer, Node>();
    private final Node                                  eastDepo;
    private final Node                                  westDepo;
    private final SortedMap<Node, SortedMap<Node, Arc>> eastboundConnections;
    private final SortedMap<Node, SortedMap<Node, Arc>> westboundConnections;
    private final Collection<Route>                     westboundRoutes;
    private final Collection<Route>                     eastboundRoutes;

    public Network(final Collection<Node> nodes, final Collection<Arc> edges) {
        this.visualizer = new GraphVisualizer(edges);
        for (final Node n : nodes) {
            this.nodes.put(n.getId(), n);
        }
        // now map every connection node
        final SortedMap<Node, SortedMap<Node, Arc>> tmpEastboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        final SortedMap<Node, SortedMap<Node, Arc>> tmpWestboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        for (final Arc a : edges) {
            if (tmpEastboundConnections.get(a.getWestNode()) == null) {
                tmpEastboundConnections.put(a.getWestNode(), new TreeMap<Node, Arc>());
            }
            tmpEastboundConnections.get(a.getWestNode()).put(a.getEastNode(), a);
            if (tmpWestboundConnections.get(a.getEastNode()) == null) {
                tmpWestboundConnections.put(a.getEastNode(), new TreeMap<Node, Arc>());
            }
            tmpWestboundConnections.get(a.getEastNode()).put(a.getWestNode(), a);
        }
        this.eastboundConnections = Collections.unmodifiableSortedMap(tmpEastboundConnections);
        this.westboundConnections = Collections.unmodifiableSortedMap(tmpWestboundConnections);
        this.eastDepo = this.locateEastboundDepo();
        this.westDepo = this.locateWestboundDepo();
        Route.resetRouteCounter();
        this.eastboundRoutes = this.getAllRoutes(new Route(Direction.EASTBOUND),
                this.eastboundConnections, this.westDepo);
        this.westboundRoutes = this.getAllRoutes(new Route(Direction.WESTBOUND),
                this.westboundConnections, this.eastDepo);
    }

    public synchronized Collection<Route> getAllEastboundRoutes() {
        return this.eastboundRoutes;
    }

    /**
     * Some of the tests depend on the exact ordering of routes produced by this method.
     * 
     * FIXME test for ordering of this method
     * 
     * @param r
     * @param connections
     * @param startingNode
     * @return
     */
    private Collection<Route> getAllRoutes(final Route r,
            final SortedMap<Node, SortedMap<Node, Arc>> connections, final Node startingNode) {
        final Collection<Route> routes = new LinkedList<Route>();
        if (connections.get(startingNode) == null) {
            return routes;
        }
        // traverse all the nodes in a defined order, create new routes from them
        final SortedSet<Node> keys = new TreeSet<Node>(connections.get(startingNode).keySet());
        for (final Node n : keys) {
            final Node nextNode = n;
            final Arc edge = connections.get(startingNode).get(n);
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

    @Override
    public boolean visualize(final File target) {
        try (FileOutputStream fos = new FileOutputStream(target)) {
            Network.logger.info("Starting visualizing network.");
            this.visualizer.visualize(fos);
            Network.logger.info("Network vizualization finished.");
            return true;
        } catch (final Exception ex) {
            Network.logger.error("Visualizing network failed.", ex);
            return false;
        }
    }
}

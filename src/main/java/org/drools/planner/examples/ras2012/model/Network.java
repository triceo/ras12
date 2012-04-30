package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.interfaces.Visualizable;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.util.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Network implements Visualizable {

    private static final Logger     logger     = LoggerFactory.getLogger(Network.class);

    private final GraphVisualizer   visualizer;
    private final Collection<Route> westboundRoutes;
    private final Collection<Route> eastboundRoutes;
    private final Map<Train, Route> bestRoutes = new HashMap<Train, Route>();

    public Network(final Collection<Node> nodes, final Collection<Arc> edges) {
        this.visualizer = new GraphVisualizer(edges);
        // now map every connection node
        final SortedMap<Node, SortedMap<Node, Arc>> eastboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        final SortedMap<Node, SortedMap<Node, Arc>> westboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        final Route eastbound = new Route(true);
        for (final Arc a : edges) {
            final Node east = a.getDestination(eastbound);
            final Node west = a.getOrigin(eastbound);
            if (eastboundConnections.get(west) == null) {
                eastboundConnections.put(west, new TreeMap<Node, Arc>());
            }
            eastboundConnections.get(west).put(east, a);
            if (westboundConnections.get(east) == null) {
                westboundConnections.put(east, new TreeMap<Node, Arc>());
            }
            westboundConnections.get(east).put(west, a);
        }
        Node eastDepo = null;
        for (final Node n : nodes) {
            if (!eastboundConnections.containsKey(n)) {
                eastDepo = n;
                break;
            }
        }
        Node westDepo = null;
        for (final Node n : nodes) {
            if (!westboundConnections.containsKey(n)) {
                westDepo = n;
                break;
            }
        }

        Route.resetRouteCounter();
        this.eastboundRoutes = this.getAllRoutes(new Route(true), eastboundConnections, westDepo);
        this.westboundRoutes = this.getAllRoutes(new Route(false), westboundConnections, eastDepo);
    }

    public Collection<Route> getAllRoutes() {
        final Collection<Route> routes = new LinkedList<Route>();
        routes.addAll(this.eastboundRoutes);
        routes.addAll(this.westboundRoutes);
        return Collections.unmodifiableCollection(routes);
    }

    /**
     * Some of the tests depend on the exact ordering of routes produced by this method.
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
            if (r.getProgression().contains(edge)) {
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

    public Route getBestRoute(final Train t) {
        if (!this.bestRoutes.containsKey(t)) {
            final SortedSet<Route> routes = new TreeSet<Route>(this.getRoutes(t));
            this.bestRoutes.put(t, routes.last());
        }
        return this.bestRoutes.get(t);
    }

    public synchronized Collection<Route> getRoutes(final Train t) {
        final Collection<Route> routes = t.isEastbound() ? this.eastboundRoutes
                : this.westboundRoutes;
        final Collection<Route> properRoutes = new LinkedHashSet<Route>(routes);
        for (final Route r : routes) {
            if (!r.isPossibleForTrain(t)) {
                properRoutes.remove(r);
            }
        }
        if (properRoutes.size() == 0) {
            // this is most probably a bug in the algorithm
            throw new IllegalStateException("No routes for train: " + t.getName());
        }
        return Collections.unmodifiableCollection(properRoutes);
    }

    @Override
    public boolean visualize(final File target) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            Network.logger.info("Starting visualizing network.");
            this.visualizer.visualize(os);
            Network.logger.info("Network vizualization finished.");
            return true;
        } catch (final Exception ex) {
            Network.logger.error("Visualizing network failed.", ex);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (final IOException e) {
                    // nothing to do here
                }
            }
        }
    }
}

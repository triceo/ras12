package org.drools.planner.examples.ras2012.util.model;

import java.io.File;
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

import org.drools.planner.examples.ras2012.Visualizable;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Route.Builder;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.visualizer.GraphVisualizer;

public class Network extends Visualizable {

    private final GraphVisualizer   visualizer;
    private final Collection<Route> westboundRoutes;
    private final Collection<Route> eastboundRoutes;
    private final Map<Train, Route> bestRoutes = new HashMap<Train, Route>();

    public Network(final Collection<Node> nodes, final Collection<Arc> edges) {
        this.visualizer = new GraphVisualizer(edges);
        // now map every connection node
        final SortedMap<Node, SortedMap<Node, Arc>> eastboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        final SortedMap<Node, SortedMap<Node, Arc>> westboundConnections = new TreeMap<Node, SortedMap<Node, Arc>>();
        final Route eastbound = new Builder(true).build();
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
        if (eastDepo == null || westDepo == null) {
            throw new IllegalStateException("Cannot find depot in one of the directions.");
        }
        this.eastboundRoutes = this.getAllRoutes(new Builder(true), eastboundConnections, westDepo);
        this.westboundRoutes = this
                .getAllRoutes(new Builder(false), westboundConnections, eastDepo);
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
    private Collection<Route> getAllRoutes(final Builder b,
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
            if (b.isAdded(edge)) {
                continue; // we'we been there already; skip this branch
            }
            final Builder newBuilder = b.add(edge);
            final Collection<Route> newRoutes = this
                    .getAllRoutes(newBuilder, connections, nextNode);
            if (newRoutes.size() > 0) {
                routes.addAll(newRoutes);
            } else {
                routes.add(newBuilder.build());
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
        return this.visualize(this.visualizer, target);
    }
}

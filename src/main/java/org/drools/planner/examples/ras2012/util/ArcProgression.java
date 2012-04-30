package org.drools.planner.examples.ras2012.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.interfaces.Directed;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Track;

public class ArcProgression implements Directed {

    private final LinkedList<Arc>   orderedArcs        = new LinkedList<Arc>();
    private final Map<Node, Arc>    arcsPerOrigin      = new HashMap<Node, Arc>();
    private final Map<Node, Arc>    arcsPerDestination = new HashMap<Node, Arc>();
    private final Map<Arc, Boolean> isArcPreferred     = new HashMap<Arc, Boolean>();
    private final Collection<Node>  nodes              = new LinkedHashSet<Node>();
    private final Collection<Node>  waitPoints;
    private final Directed          directed;

    public ArcProgression(final Directed directed, final Arc... arcs) {
        this(directed, Arrays.asList(arcs));
    }

    public ArcProgression(final Directed directed, final Collection<Arc> arcs) {
        this.directed = directed;
        // put arcs in proper order
        Node startingNode = this.getStartingNode(directed, arcs);
        while (arcs.size() != this.orderedArcs.size()) {
            for (final Arc a : arcs) {
                if (a.getOrigin(directed) == startingNode) {
                    this.orderedArcs.addLast(a);
                    startingNode = this.orderedArcs.peekLast().getDestination(directed);
                    break;
                }
            }
        }
        // cache information about nodes related to arcs
        for (final Arc a : this.orderedArcs) {
            this.arcsPerOrigin.put(a.getOrigin(this), a);
            this.arcsPerDestination.put(a.getDestination(this), a);
            this.nodes.add(a.getOrigin(this));
            this.nodes.add(a.getDestination(this));
        }
        // determine whether a particular arc is preferred
        for (final Arc a : this.orderedArcs) {
            this.isArcPreferred.put(a, this.determineArcPreferrence(a));
        }
        // and finally cache the wait points
        this.waitPoints = this.assembleWaitPoints();
    }

    private Collection<Node> assembleWaitPoints() {
        final Collection<Node> points = new TreeSet<Node>();
        if (this.orderedArcs.size() == 0) {
            return Collections.unmodifiableCollection(points);
        }
        // we want to be able to hold the train before it enters the network
        final Arc firstArc = this.getOrigin();
        points.add(firstArc.getOrigin(this));
        // other wait points depend on the type of the track
        for (final Arc a : this.orderedArcs) {
            if (a.getTrack() == Track.SIDING) {
                // on sidings, wait before leaving them through a switch
                points.add(a.getDestination(this));
            } else if (!a.getTrack().isMainTrack()) {
                // on crossovers and switches, wait before joining them
                points.add(a.getOrigin(this));
            } else {
                // on main tracks, never wait
            }
        }
        return Collections.unmodifiableCollection(points);
    }

    public boolean contains(final Arc a) {
        return this.orderedArcs.contains(a);
    }

    public int countArcs() {
        return this.orderedArcs.size();
    }

    private boolean determineArcPreferrence(final Arc a) {
        if (a.getTrack() == Track.MAIN_0) {
            return true;
        } else if (a.getTrack() == Track.MAIN_2) {
            return this.isEastbound();
        } else if (a.getTrack() == Track.MAIN_1) {
            return this.isWestbound();
        } else {
            // preference of SIDING/SWITCH/CROSSOVER is based on which track are those coming off of
            final Arc previousArc = this.getPrevious(a);
            if (previousArc == null) {
                return true;
            } else {
                return this.determineArcPreferrence(previousArc);
            }
        }
    }

    public List<Arc> getArcs() {
        return Collections.unmodifiableList(this.orderedArcs);
    }

    public Arc getDestination() {
        return this.orderedArcs.peekLast();
    }

    public Arc getNext(final Arc a) {
        if (this.orderedArcs.size() == 0) {
            throw new IllegalArgumentException("No next arc on an empty route.");
        } else if (a == null) {
            return this.getOrigin();
        }
        final int indexOf = this.orderedArcs.indexOf(a);
        if (indexOf < 0) {
            throw new IllegalArgumentException("Arc not in the progression!");
        } else if (indexOf == this.orderedArcs.size() - 1) {
            return null;
        } else {
            return this.orderedArcs.get(indexOf + 1);
        }

    }

    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(this.nodes);
    }

    public Arc getOrigin() {
        return this.orderedArcs.peekFirst();
    }

    public Arc getPrevious(final Arc a) {
        if (this.orderedArcs.size() == 0) {
            throw new IllegalArgumentException("No previous arc on an empty route.");
        } else if (a == null) {
            return this.getDestination();
        }
        final int indexOf = this.orderedArcs.indexOf(a);
        if (indexOf < 0) {
            throw new IllegalArgumentException("Arc not in the progression!");
        } else if (indexOf == 0) {
            return null;
        } else {
            return this.orderedArcs.get(indexOf - 1);
        }

    }

    private Node getStartingNode(final Directed r, final Collection<Arc> arcs) {
        final Set<Node> isDestination = new HashSet<Node>();
        final Set<Node> isOrigin = new HashSet<Node>();
        for (final Arc a : arcs) {
            isOrigin.add(a.getOrigin(r));
            isDestination.add(a.getDestination(r));
        }
        isOrigin.removeAll(isDestination);
        if (isOrigin.size() == 1) {
            return isOrigin.toArray(new Node[1])[0];
        } else {
            return null;
        }
    }

    public Collection<Node> getWaitPoints() {
        return this.waitPoints;
    }

    public Arc getWithDestinationNode(final Node n) {
        return this.arcsPerDestination.get(n);
    }

    public Arc getWithOriginNode(final Node n) {
        return this.arcsPerOrigin.get(n);
    }

    /**
     * Returns arc progression that is a subset of this arc progression. The subset starts at the beginning and ends with the
     * arc that has the specified node as its ending.
     * 
     * @param n Node to end the arc progression.
     * @return Subset from nodes 0 to n. If the node is the ending node, returns this. If the node is the starting node, returns
     *         empty progression.
     */
    public ArcProgression head(final Node n) {
        if (!this.nodes.contains(n)) {
            throw new IllegalArgumentException("Node not in progression!");
        } else if (n == this.getDestination().getDestination(this)) {
            return this;
        } else if (n == this.getOrigin().getOrigin(this)) {
            return new ArcProgression(this, new Arc[0]);
        }
        final Arc a = this.getWithDestinationNode(n);
        assert a != null;
        final int indexOf = this.orderedArcs.indexOf(a);
        return new ArcProgression(this, this.orderedArcs.subList(0, indexOf + 1));
    }

    @Override
    public boolean isEastbound() {
        return this.directed.isEastbound();
    }

    public boolean isPreferred(final Arc a) {
        return this.isArcPreferred.get(a);
    }

    @Override
    public boolean isWestbound() {
        return this.directed.isWestbound();
    }

    /**
     * Returns arc progression that is a subset of this arc progression. The subset starts with the arc that has the specified
     * node as its beginning, ends where this progression ends.
     * 
     * @param n Node to start the arc progression.
     * @return Subset from nodes n to end. If the node is the ending node, returns empty progression. If the node is the
     *         starting node, returns this.
     */
    public ArcProgression tail(final Node n) {
        if (!this.nodes.contains(n)) {
            throw new IllegalArgumentException("Node not in progression!");
        } else if (n == this.getDestination().getDestination(this)) {
            return new ArcProgression(this, new Arc[0]);
        } else if (n == this.getOrigin().getOrigin(this)) {
            return this;
        }
        final Arc a = this.getWithOriginNode(n);
        assert a != null;
        final int indexOf = this.orderedArcs.indexOf(a);
        return new ArcProgression(this, this.orderedArcs.subList(indexOf, this.orderedArcs.size()));
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ArcProgression [orderedArcs=").append(this.orderedArcs).append("]");
        return builder.toString();
    }
}

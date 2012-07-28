package org.drools.planner.examples.ras2012.util.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Track;

public class ArcProgression implements Directed {

    private final List<Arc>                  orderedArcs        = new ArrayList<Arc>();
    private final SortedMap<BigDecimal, Arc> milestones         = new TreeMap<BigDecimal, Arc>();
    private final Map<Node, Arc>             arcsPerOrigin      = new LinkedHashMap<Node, Arc>();
    private final Map<Node, Arc>             arcsPerDestination = new LinkedHashMap<Node, Arc>();
    private final Map<Arc, Arc>              nextArcs           = new LinkedHashMap<Arc, Arc>();
    private final Map<Arc, Arc>              previousArcs       = new LinkedHashMap<Arc, Arc>();
    private final Map<Node, Node>            nextNodes          = new LinkedHashMap<Node, Node>();
    private final Map<Node, Node>            previousNodes      = new LinkedHashMap<Node, Node>();
    private final Map<Arc, Boolean>          isArcPreferred     = new LinkedHashMap<Arc, Boolean>();
    private final List<Node>                 nodes              = new ArrayList<Node>();
    private final Collection<Node>           waitPoints;
    private final boolean                    isEastbound;
    private final boolean                    isEmpty;
    private final BigDecimal                 length;

    private final Map<Node, BigDecimal>      distanceCache      = new HashMap<Node, BigDecimal>();

    public ArcProgression(final Directed directed, final Arc... arcs) {
        this(directed, Arrays.asList(arcs));
    }

    public ArcProgression(final Directed directed, final Collection<Arc> arcs) {
        this.isEastbound = directed.isEastbound();
        // put arcs in proper order
        Node startingNode = this.getStartingNode(directed, arcs);
        while (arcs.size() != this.orderedArcs.size()) {
            for (final Arc a : arcs) {
                if (a.getOrigin(directed) == startingNode) {
                    this.orderedArcs.add(a);
                    // find the node of the last arc in the progression
                    startingNode = a.getDestination(directed);
                    break;
                }
            }
        }
        // cache information about nodes related to arcs
        BigDecimal milestone = BigDecimal.ZERO;
        Arc previousArc = null;
        for (final Arc a : this.orderedArcs) {
            this.milestones.put(milestone, a);
            this.arcsPerOrigin.put(a.getOrigin(this), a);
            this.arcsPerDestination.put(a.getDestination(this), a);
            this.previousArcs.put(a, previousArc);
            this.nextArcs.put(previousArc, a);
            this.nextNodes.put(a.getOrigin(this), a.getDestination(this));
            this.previousNodes.put(a.getDestination(this), a.getOrigin(this));
            this.nodes.add(a.getOrigin(this));
            milestone = milestone.add(a.getLength());
            previousArc = a;
        }
        this.length = milestone;
        this.isEmpty = this.orderedArcs.size() == 0;
        if (!this.isEmpty) {
            this.previousNodes.put(this.getOrigin().getOrigin(this), null);
            this.nextNodes.put(this.getDestination().getDestination(this), null);
            this.previousArcs.put(this.getOrigin(), null);
            this.nextArcs.put(this.getDestination(), null);
            this.nodes.add(this.getDestination().getDestination(this));
        }
        // determine whether a particular arc is preferred
        for (final Arc a : this.orderedArcs) {
            this.isArcPreferred.put(a, this.determineArcPreferrence(a));
        }
        // and finally cache the wait points
        this.waitPoints = this.assembleWaitPoints();
    }

    @SuppressWarnings("unchecked")
    private Collection<Node> assembleWaitPoints() {
        if (this.isEmpty) {
            return Collections.unmodifiableSet(Collections.EMPTY_SET);
        }
        final Collection<Node> points = new TreeSet<Node>();
        // we want to be able to hold the train before it enters the network
        final Arc firstArc = this.getOrigin();
        points.add(firstArc.getOrigin(this));
        // other wait points depend on the type of the track
        for (final Arc a : this.orderedArcs) {
            switch (a.getTrack()) {
                case SIDING:
                    // on sidings, wait before leaving them through a switch
                    points.add(a.getDestination(this));
                    break;
                case CROSSOVER:
                    // on crossovers, wait before joining them
                    points.add(a.getOrigin(this));
                    break;
                default:
                    /*
                     * we don't wait on main tracks not to block them; also, we don't wait on switches, since then the train is
                     * reaching over to a main track and blocking it. sidings are close enough to not need to wait at switches.
                     */
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
            final Arc previousArc = this.getPreviousArc(a);
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
        if (this.isEmpty) {
            throw new IllegalStateException("Empty progression has no destination.");
        }
        return this.orderedArcs.get(this.orderedArcs.size() - 1);
    }

    public BigDecimal getDistance(final Node end) {
        final BigDecimal cached = this.distanceCache.get(end);
        if (cached != null) {
            return cached;
        }
        if (end == this.getOrigin().getOrigin(this)) {
            return BigDecimal.ZERO;
        } else {
            final Arc a = this.arcsPerDestination.get(end);
            if (a == null) {
                throw new IllegalArgumentException(end + " not in progression.");
            }
            this.distanceCache.put(end,
                    this.getDistance(this.getPreviousNode(end)).add(a.getLength()));
            return this.distanceCache.get(end);
        }
    }

    public BigDecimal getLength() {
        return this.length;
    }

    public Arc getNextArc(final Arc a) {
        if (this.isEmpty) {
            throw new IllegalArgumentException("No next arc on an empty route.");
        } else if (a == null) {
            return this.getOrigin();
        }
        if (this.nextArcs.containsKey(a)) {
            return this.nextArcs.get(a);
        } else {
            throw new IllegalArgumentException(a + " not in the progression!");
        }
    }

    public Node getNextNode(final Node n) {
        if (this.isEmpty) {
            throw new IllegalArgumentException("No next node on an empty route.");
        } else if (n == null) {
            return this.getOrigin().getOrigin(this);
        }
        if (this.nextNodes.containsKey(n)) {
            return this.nextNodes.get(n);
        } else {
            throw new IllegalArgumentException(n + " not in the progression!");
        }
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public Arc getOrigin() {
        if (this.isEmpty) {
            throw new IllegalStateException("Empty progression has no origin.");
        }
        return this.orderedArcs.get(0);
    }

    public Arc getPreviousArc(final Arc a) {
        if (this.isEmpty) {
            throw new IllegalArgumentException("No previous arc on an empty route.");
        } else if (a == null) {
            return this.getDestination();
        }
        if (this.previousArcs.containsKey(a)) {
            return this.previousArcs.get(a);
        } else {
            throw new IllegalArgumentException(a + " not in the progression!");
        }
    }

    public Node getPreviousNode(final Node n) {
        if (this.isEmpty) {
            throw new IllegalArgumentException("No previous node on an empty route.");
        } else if (n == null) {
            return this.getDestination().getDestination(this);
        }
        if (this.previousNodes.containsKey(n)) {
            return this.previousNodes.get(n);
        } else {
            throw new IllegalArgumentException(n + " not in the progression!");
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

    @Override
    public boolean isEastbound() {
        return this.isEastbound;
    }

    public boolean isPreferred(final Arc a) {
        return this.isArcPreferred.get(a);
    }

    @Override
    public boolean isWestbound() {
        return !this.isEastbound();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ArcProgression [orderedArcs=").append(this.orderedArcs).append("]");
        return builder.toString();
    }
}

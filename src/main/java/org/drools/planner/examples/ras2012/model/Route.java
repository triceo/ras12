package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.interfaces.Directed;
import org.drools.planner.examples.ras2012.interfaces.Visualizable;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Arc.TrackType;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.ScheduleAdherenceRequirement;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.util.RouteVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Route implements Comparable<Route>, Directed, Visualizable {

    private static final Logger        logger      = LoggerFactory.getLogger(Route.class);

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    protected static int resetRouteCounter() {
        return Route.idGenerator.getAndSet(0);
    }

    private final List<Arc>           arcs                    = new LinkedList<Arc>();

    private final boolean             isEastbound;
    private final Arc                 origin, destination;

    private final int                 id                      = Route.idGenerator.getAndIncrement();

    private int                       numberOfPreferredTracks = -1;

    private BigDecimal                travellingTimeInMinutes = null;

    private final Map<Train, Boolean> routePossibilitiesCache = new HashMap<Train, Boolean>();

    public Route(final boolean isEastbound) {
        this(isEastbound, null, null);
    }

    private Route(final boolean isEastbound, final Arc... e) {
        this.isEastbound = isEastbound;
        this.origin = e[0];
        this.destination = e[e.length - 1];
        for (final Arc a : e) {
            if (a != null) {
                this.arcs.add(a);
            }
        }
    }

    @Override
    public int compareTo(final Route o) {
        if (this.isEastbound == o.isEastbound) { // less tracks = better
            final int comparison = o.getLengthInArcs() - this.getLengthInArcs();
            if (comparison == 0) { // shorter = better
                final int comparison2 = o.getTravellingTimeInMinutes().compareTo(
                        this.getTravellingTimeInMinutes());
                if (comparison2 == 0) { // more preferred tracks = better
                    return this.getNumberOfPreferredTracks() - o.getNumberOfPreferredTracks();
                } else {
                    return comparison2;
                }
            } else {
                return comparison;
            }
        } else {
            if (this.isEastbound()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    protected boolean contains(final Arc e) {
        return this.arcs.contains(e);
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
        final Route other = (Route) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public Route extend(final Arc add) {
        if (add == null) {
            throw new IllegalArgumentException("Cannot extend route with a null arc!");
        }
        if (this.arcs.contains(add)) {
            throw new IllegalArgumentException("Cannot extend route with the same arc twice!");
        }
        final List<Arc> newParts = new ArrayList<Arc>(this.arcs);
        newParts.add(add);
        final Arc[] result = newParts.toArray(new Arc[newParts.size()]);
        return new Route(this.isEastbound(), result);
    }

    private Arc getAdjacentArc(final Arc arc, final boolean next) {
        if (this.arcs.isEmpty()) {
            throw new IllegalArgumentException("There are no arcs in an empty route.");
        } else if (arc == null) {
            if (next) {
                return this.getOrigin();
            } else {
                return this.getDestination();
            }
        } else if (!this.contains(arc)) {
            throw new IllegalArgumentException("The route doesn't contain the arc.");
        }
        final int index = this.arcs.indexOf(arc);
        final int leftIndex = index - 1;
        final Arc arcLeftOf = leftIndex < 0 ? null : this.arcs.get(leftIndex);
        final int rightIndex = index + 1;
        final Arc arcRightOf = rightIndex < this.arcs.size() ? this.arcs.get(rightIndex) : null;
        if (next) {
            if (this.isNextArc(arc, arcLeftOf)) {
                return arcLeftOf;
            } else if (this.isNextArc(arc, arcRightOf)) {
                return arcRightOf;
            }
        } else {
            if (this.isPreviousArc(arc, arcLeftOf)) {
                return arcLeftOf;
            } else if (this.isPreviousArc(arc, arcRightOf)) {
                return arcRightOf;
            }
        }
        return null;
    }

    public List<Arc> getArcs() {
        return this.arcs;
    }

    public Arc getDestination() {
        if (this.destination == null) {
            return this.origin;
        }
        return this.destination;
    }

    public int getId() {
        return this.id;
    }

    private int getLengthInArcs() {
        return this.arcs.size();
    }

    public Arc getNextArc(final Arc a) {
        return this.getAdjacentArc(a, true);
    }

    private int getNumberOfPreferredTracks() {
        if (this.numberOfPreferredTracks == -1) {
            int i = 0;
            for (final Arc a : this.arcs) {
                if (this.isArcPreferred(a)) {
                    i++;
                }
            }
            this.numberOfPreferredTracks = i;
        }
        return this.numberOfPreferredTracks;
    }

    public Arc getOrigin() {
        return this.origin;
    }

    public Arc getPreviousArc(final Arc a) {
        return this.getAdjacentArc(a, false);
    }

    private BigDecimal getTravellingTimeInMinutes() {
        if (this.travellingTimeInMinutes == null) {
            BigDecimal result = BigDecimal.ZERO;
            for (final Arc a : this.arcs) {
                final BigDecimal length = a.getLengthInMiles();
                final int speed = this.isEastbound() ? a.getTrackType().getSpeedEastbound() : a
                        .getTrackType().getSpeedWestbound();
                final BigDecimal timeInHours = length.divide(BigDecimal.valueOf(speed), 2,
                        BigDecimal.ROUND_HALF_DOWN);
                result = result.add(timeInHours.multiply(BigDecimal.valueOf(60)));
            }
            this.travellingTimeInMinutes = result;
        }
        return this.travellingTimeInMinutes;
    }

    public Collection<Node> getWaitPoints() {
        final Collection<Node> waitPoints = new TreeSet<Node>();
        // we want to be able to hold the train before it enters the network
        final Arc firstArc = this.getOrigin();
        waitPoints.add(firstArc.getOrigin(this));
        // other wait points depend on the type of the track
        for (final Arc a : this.arcs) {
            if (a.getTrackType() == TrackType.SIDING) {
                // on sidings, wait before leaving them through a switch
                waitPoints.add(a.getDestination(this));
            } else if (!a.getTrackType().isMainTrack()) {
                // on crossovers and switches, wait before joining them
                waitPoints.add(a.getOrigin(this));
            } else {
                // on main tracks, never wait
            }
        }
        return Collections.unmodifiableCollection(waitPoints);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        return result;
    }

    public boolean isArcPreferred(final Arc a) {
        if (a.getTrackType() == TrackType.MAIN_0) {
            return true;
        } else if (a.getTrackType() == TrackType.MAIN_2) {
            return this.isEastbound();
        } else if (a.getTrackType() == TrackType.MAIN_1) {
            return this.isWestbound();
        } else {
            // preference of SIDING/SWITCH/CROSSOVER is based on which track are those coming off of
            final Arc previousArc = this.getPreviousArc(a);
            if (previousArc == null) {
                return true;
            } else {
                return this.isArcPreferred(previousArc);
            }
        }
    }

    @Override
    public boolean isEastbound() {
        return this.isEastbound;
    }

    private boolean isNextArc(final Arc current, final Arc supposedlyNext) {
        if (supposedlyNext == null) {
            return current == this.getDestination();
        }
        return current.getDestination(this) == supposedlyNext.getOrigin(this);
    }

    public boolean isPossibleForTrain(final Train t) {
        if (!this.routePossibilitiesCache.containsKey(t)) {
            this.routePossibilitiesCache.put(t, this.isPossibleForTrainUncached(t));
        }
        return this.routePossibilitiesCache.get(t);
    }

    private boolean isPossibleForTrainUncached(final Train t) {
        // make sure both the route and the train are in the same direction
        final boolean bothEastbound = t.isEastbound() && this.isEastbound();
        final boolean bothWestbound = t.isWestbound() && this.isWestbound();
        if (!(bothEastbound || bothWestbound)) {
            return false;
        }
        boolean containsOrigin = false;
        boolean containsDestination = false;
        for (final Arc a : this.arcs) {
            containsOrigin = containsOrigin || a.getOrigin(t) == t.getOrigin();
            containsDestination = containsDestination || a.getDestination(t) == t.getDestination();
            if (a.getTrackType() == TrackType.SIDING) {
                if (t.isHeavy()) {
                    /*
                     * heavy trains must never use a siding when there is a meet-pass with another NSA train. this is the
                     * easiest way to fulfill the requirement.
                     */
                    return false;
                }
                // hazmat trains disallowed to take sidings
                if (t.carriesHazardousMaterials()) {
                    return false;
                }
                // make sure the route doesn't contain a siding shorter than the train
                final int result = a.getLengthInMiles().compareTo(t.getLength());
                if (result < 0) {
                    return false;
                }
            }
        }
        /*
         * some trains don't enter the world at depots. we must make sure that their origin/destination is actually part of this
         * route
         */
        if (!containsOrigin || !containsDestination) {
            return false;
        }
        // make sure that the train traverses through everywhere it's expected to
        for (final ScheduleAdherenceRequirement sar : t.getScheduleAdherenceRequirements()) {
            final Node requestedNode = sar.getDestination();
            boolean found = false;
            for (final Arc a : this.arcs) {
                if (a.getOrigin(t) == requestedNode || a.getDestination(t) == requestedNode) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean isPreviousArc(final Arc current, final Arc supposedlyPrevious) {
        if (supposedlyPrevious == null) {
            return current == this.getOrigin();
        }
        return current.getOrigin(this) == supposedlyPrevious.getDestination(this);
    }

    @Override
    public boolean isWestbound() {
        return !this.isEastbound();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Route [id=").append(this.id).append(", isEastbound=")
                .append(this.isEastbound).append(", arcs=").append(this.arcs).append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            final Collection<Node> nodes = new HashSet<Node>();
            for (final Arc a : this.arcs) {
                nodes.add(a.getOrigin(this));
                nodes.add(a.getDestination(this));
            }
            Route.logger.info("Starting visualizing route: " + this.getId());
            new RouteVisualizer(this).visualize(os);
            Route.logger.info("Route visualization finished: " + this.getId());
            return true;
        } catch (final Exception ex) {
            Route.logger.error("Visualizing route " + this.getId() + " failed.", ex);
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

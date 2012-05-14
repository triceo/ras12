package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.ScheduleAdherenceRequirement;
import org.drools.planner.examples.ras2012.model.original.Track;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.util.ArcProgression;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.RouteVisualizer;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Route extends Visualizable implements Comparable<Route>, Directed {

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    protected static int resetRouteCounter() {
        return Route.idGenerator.getAndSet(0);
    }

    private final boolean             isEastbound;
    private final ArcProgression      progression;

    private final int                 id                           = Route.idGenerator
                                                                           .getAndIncrement();

    private int                       numberOfPreferredTracks      = -1;

    private long                      travellingTimeInMilliseconds = -1;

    private final Map<Train, Boolean> routePossibilitiesCache      = new HashMap<Train, Boolean>();

    public Route(final boolean isEastbound) {
        this(isEastbound, new Arc[0]);
    }

    private Route(final boolean isEastbound, final Arc... e) {
        this.isEastbound = isEastbound;
        this.progression = new ArcProgression(this, e);
    }

    @Override
    public int compareTo(final Route o) {
        if (this.isEastbound == o.isEastbound) { // less tracks = better
            final int comparison = o.progression.countArcs() - this.progression.countArcs();
            if (comparison == 0) { // shorter = better
                if (this.getTravellingTimeInMillis() == o.getTravellingTimeInMillis()) { // more preferred tracks = better
                    return this.getNumberOfPreferredTracks() - o.getNumberOfPreferredTracks();
                } else {
                    return o.getTravellingTimeInMillis() > this.getTravellingTimeInMillis() ? 1
                            : -1;
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
        if (this.progression.contains(add)) {
            throw new IllegalArgumentException("Cannot extend route with the same arc twice!");
        }
        final List<Arc> newParts = new ArrayList<Arc>(this.progression.getArcs());
        newParts.add(add);
        final Arc[] result = newParts.toArray(new Arc[newParts.size()]);
        return new Route(this.isEastbound(), result);
    }

    public int getId() {
        return this.id;
    }

    private int getNumberOfPreferredTracks() {
        if (this.numberOfPreferredTracks == -1) {
            int i = 0;
            for (final Arc a : this.progression.getArcs()) {
                if (this.getProgression().isPreferred(a)) {
                    i++;
                }
            }
            this.numberOfPreferredTracks = i;
        }
        return this.numberOfPreferredTracks;
    }

    public ArcProgression getProgression() {
        return this.progression;
    }

    private long getTravellingTimeInMillis() {
        if (this.travellingTimeInMilliseconds == -1) {
            long result = 0;
            for (final Arc a : this.progression.getArcs()) {
                final BigDecimal length = a.getLengthInMiles();
                final int speed = this.isEastbound() ? a.getTrack().getSpeedEastbound() : a
                        .getTrack().getSpeedWestbound();
                result += Converter.getTimeFromSpeedAndDistance(speed, length);
            }
            this.travellingTimeInMilliseconds = result;
        }
        return this.travellingTimeInMilliseconds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        return result;
    }

    @Override
    public boolean isEastbound() {
        return this.isEastbound;
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
        /*
         * some trains don't enter the world at depots. we must make sure that their origin/destination is actually part of this
         * route
         */
        final Collection<Node> nodes = this.progression.getNodes();
        if (!nodes.contains(t.getOrigin()) || !nodes.contains(t.getDestination())) {
            return false;
        }
        // make sure that the route leads through every node where the train is expected
        for (final ScheduleAdherenceRequirement sar : t.getScheduleAdherenceRequirements()) {
            if (!nodes.contains(sar.getDestination())) {
                return false;
            }
        }
        // now traverse arcs to make sure every other condition is met
        for (final Arc a : this.progression.getArcs()) {
            if (a.getTrack() != Track.SIDING) { // we only have rules for sidings
                continue;
            }
            if (t.isHeavy()) {
                /*
                 * heavy trains must never use a siding when there is a meet-pass with another NSA train. this is the easiest
                 * way to fulfill the requirement.
                 */
                return false;
            }
            // hazmat trains disallowed to take sidings
            if (t.carriesHazardousMaterials()) {
                return false;
            }
            // make sure the route doesn't contain a siding shorter than the train
            final int result = a.getLengthInMiles().compareTo(t.getLengthInMiles());
            if (result < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isWestbound() {
        return !this.isEastbound();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Route [id=").append(this.id).append(", isEastbound=")
                .append(this.isEastbound).append(", arcs=").append(this.progression).append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        return this.visualize(new RouteVisualizer(this), target);
    }
}

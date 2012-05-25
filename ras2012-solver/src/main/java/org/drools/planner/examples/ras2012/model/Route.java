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
import org.drools.planner.examples.ras2012.Visualizable;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.model.ArcProgression;
import org.drools.planner.examples.ras2012.util.visualizer.RouteVisualizer;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Route extends Visualizable implements Comparable<Route>, Directed {

    public static class Builder implements Directed {

        private final AtomicInteger   idGenerator;

        private final boolean         isEastbound;
        private final Collection<Arc> arcs = new ArrayList<Arc>();

        private Builder(final AtomicInteger id, final boolean isEastbound, final Arc... arcs) {
            this.idGenerator = id;
            this.isEastbound = isEastbound;
            for (final Arc a : arcs) {
                this.arcs.add(a);
            }

        }

        public Builder(final boolean isEastbound, final Arc... arcs) {
            this(isEastbound ? new AtomicInteger(0) : new AtomicInteger(1), isEastbound, arcs);
        }

        public Builder add(final Arc arc) {
            if (arc == null) {
                throw new IllegalArgumentException("Cannot extend route with a null arc!");
            }
            if (this.arcs.contains(arc)) {
                throw new IllegalArgumentException("Cannot extend route with the same arc twice!");
            }
            final List<Arc> arcs = new ArrayList<Arc>(this.arcs);
            arcs.add(arc);
            return new Builder(this.idGenerator, this.isEastbound,
                    arcs.toArray(new Arc[arcs.size()]));
        }

        public Route build() {
            return new Route(this.idGenerator.getAndAdd(2), this.isEastbound,
                    this.arcs.toArray(new Arc[this.arcs.size()]));
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Builder)) {
                return false;
            }
            final Builder other = (Builder) obj;
            if (this.isEastbound != other.isEastbound) {
                return false;
            }
            if (!this.arcs.equals(other.arcs)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.arcs == null ? 0 : this.arcs.hashCode());
            result = prime * result + (this.isEastbound ? 1231 : 1237);
            return result;
        }

        public boolean isAdded(final Arc arc) {
            return this.arcs.contains(arc);
        }

        @Override
        public boolean isEastbound() {
            return this.isEastbound;
        }

        @Override
        public boolean isWestbound() {
            return !this.isEastbound();
        }
    }

    private final ArcProgression      progression;

    private final int                 id;

    private int                       numberOfPreferredTracks      = -1;

    private long                      travellingTimeInMilliseconds = -1;

    private final Map<Train, Boolean> routePossibilitiesCache      = new HashMap<Train, Boolean>();

    private Route(final int id, final boolean isEastbound, final Arc... e) {
        this.id = id;
        this.progression = new ArcProgression(this, e);
    }

    @Override
    public int compareTo(final Route o) {
        if (this.isEastbound() == o.isEastbound()) { // less tracks = better
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
                final BigDecimal length = a.getLength();
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
        return this.getId() % 2 == 0;
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
        for (final ScheduleAdherenceRequirement sar : t.getScheduleAdherenceRequirements().values()) {
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
            final int result = a.getLength().compareTo(t.getLength());
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
                .append(this.isEastbound()).append(", arcs=").append(this.progression).append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        return this.visualize(new RouteVisualizer(this), target);
    }
}

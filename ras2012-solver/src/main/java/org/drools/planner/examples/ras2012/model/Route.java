package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.Visualizable;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.model.ArcProgression;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.drools.planner.examples.ras2012.util.visualizer.RouteVisualizer;

/**
 * Represents a way in which it is possible to travel the {@link Territory}. When constructed using {@link Builder}, it is
 * assigned an {@link ArcProgression}, which is a huge cache of various information about it.
 * 
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Route extends Visualizable implements Comparable<Route>, Directed {

    /**
     * Used to build the {@link Route} by specifying a series of {@link Arc}s.
     * 
     */
    public static class Builder implements Directed {

        private final AtomicInteger   idGenerator;

        private final boolean         isEastbound;
        private final Collection<Arc> arcs = new ArrayList<>();

        private Builder(final AtomicInteger id, final boolean isEastbound, final Arc... arcs) {
            this.idGenerator = id;
            this.isEastbound = isEastbound;
            for (final Arc a : arcs) {
                this.arcs.add(a);
            }

        }

        /**
         * Instantiate new builder to create a new series of related routes. Routes from different {@link Territory}s should use
         * different Builders.
         * 
         * @param isEastbound Whether or not the route goes east.
         * @param arcs Arcs to be placed on the route, in the intended order.
         */
        public Builder(final boolean isEastbound, final Arc... arcs) {
            this(isEastbound ? new AtomicInteger(0) : new AtomicInteger(1), isEastbound, arcs);
        }

        /**
         * Add another {@link Arc} at the end of the {@link Route}.
         * 
         * @param arc The arc in question.
         * @return For call chaining.
         */
        public Builder add(final Arc arc) {
            if (arc == null) {
                throw new IllegalArgumentException("Cannot extend route with a null arc!");
            }
            if (this.isAdded(arc)) {
                throw new IllegalArgumentException("Cannot extend route with the same arc twice!");
            }
            final List<Arc> arcs = new ArrayList<>(this.arcs);
            arcs.add(arc);
            return new Builder(this.idGenerator, this.isEastbound,
                    arcs.toArray(new Arc[arcs.size()]));
        }

        /**
         * Actually create the {@link Route} instance. {@link Arc}s will be ordered the way they were {@link #add(Arc)}ed.
         * 
         * @return The route. Odd ID when westbound, even when eastbound.
         */
        public Route build() {
            return new Route(this.idGenerator.getAndAdd(2), this.arcs.toArray(new Arc[this.arcs
                    .size()]));
        }

        /**
         * Builders only equal when they have the same {@link Arc}s in the same order and when they have the same direction.
         */
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

        /**
         * Whether or not the {@link Arc} is already on the future {@link Route}.
         * 
         * @param arc Arc in question.
         * @return True if already added.
         */
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

    private final Map<Train, Boolean> routePossibilitiesCache      = new HashMap<>();

    private final RouteVisualizer     visualizer;

    private Route(final int id, final Arc... e) {
        this.id = id;
        this.progression = new ArcProgression(this, e);
        this.visualizer = new RouteVisualizer(this);
    }

    @Override
    public int compareTo(final Route o) {
        /* calculate a route quality metric. the less time, the better; the more preferred tracks, the better. */
        final float thisMetric = (float) this.getTravellingTimeInMillis()
                / (float) this.getNumberOfPreferredTracks();
        final float otherMetric = (float) o.getTravellingTimeInMillis()
                / (float) o.getNumberOfPreferredTracks();
        return Math.round((otherMetric - thisMetric) * 1000);
    }

    /**
     * Routes only equal when they have the same ID.
     */
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
        return new EqualsBuilder().append(this.getId(), other.getId()).isEquals();
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
                result += Converter.getTimeFromSpeedAndDistance(BigDecimal.valueOf(speed), length);
            }
            this.travellingTimeInMilliseconds = result;
        }
        return this.travellingTimeInMilliseconds;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getId()).toHashCode();
    }

    @Override
    public boolean isEastbound() {
        return this.getId() % 2 == 0;
    }

    /**
     * <p>
     * Whether or not a particular {@link Train} is allowed on the route.
     * </p>
     * 
     * <p>
     * It would be forbidden, when the {@link Train}:
     * </p>
     * 
     * <ul>
     * <li>Goes the opposite direction than the route.</li>
     * <li>Is required to pass through {@link Node} that is not on this route.</li>
     * <li>Is longer than one of the sidings on this route. (Required by RAS problem description.)</li>
     * <li>Carries hazardous materials and the route contains sidings. (Required by RAS problem description.)</li>
     * <li>Is heavy and the route contains sidings. Although technically not required by the RAS problem description, this is
     * the easiest way to fulfill the requirement of never sending heavy {@link Train} to sidings when meet-passing another
     * {@link Train}.</li>
     * </ul>
     * 
     * @param t The train in question.
     * @return True if this route is possible for the {@link Train}. The results are cached.
     */
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
            final boolean isSiding = a.getTrack() == Track.SIDING;
            final boolean isSwitch = a.getTrack() == Track.SWITCH;
            if (!isSwitch && !isSiding) { // we only have rules for sidings/switches
                continue;
            }
            if (t.isHeavy()) {
                /*
                 * heavy trains must never use a siding/switch when there is a meet-pass with another NSA train. this is the
                 * easiest way to fulfill the requirement.
                 */
                return false;
            }
            // hazmat trains disallowed to take sidings/switches
            if (t.carriesHazardousMaterials()) {
                return false;
            }
            // make sure the route doesn't contain a siding shorter than the train
            if (isSwitch) {
                continue;
            }
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
        return this.visualize(this.visualizer, target);
    }
}

package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.drools.planner.examples.ras2012.Visualizable;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.model.ArcProgression;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.drools.planner.examples.ras2012.util.visualizer.ItineraryVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the way a {@link Train} travels the {@link Route}, including all the stops for {@link MaintenanceWindow}s and
 * {@link WaitTime}s. It is basically a schedule for the train on the {@link Territory}. Instances of the class cache most of
 * their data, only updating it when a {@link WaitTime} change occurs. (See {@link #setWaitTime(Node, WaitTime)} and
 * {@link #removeWaitTime(Node)}.)
 * 
 * This class has no concept of planning horizon. It always calculates the whole schedule, from the train entry to train
 * reaching destination.
 */
public final class Itinerary extends Visualizable {

    private static final TimeUnit              DEFAULT_TIME_UNIT     = TimeUnit.MILLISECONDS;

    private final Route                        route;

    private final Train                        train;

    private final AtomicBoolean                scheduleCacheValid    = new AtomicBoolean(false);
    private final Collection<Node>             hasNodes              = new LinkedHashSet<>();
    private SortedMap<Long, Node>              scheduleCache         = new TreeMap<>();
    private SortedMap<Long, Arc>               scheduleCacheWithArcs = new TreeMap<>();
    private final long                         trainEntryTime;
    private final Map<Node, WaitTime>          nodeWaitTimes         = new HashMap<>();

    // FIXME only one window per node; multiple different windows with same node will get lost
    private final Map<Node, MaintenanceWindow> maintenances          = new HashMap<>();

    private static final Logger                logger                = LoggerFactory
                                                                             .getLogger(Itinerary.class);

    /**
     * Create schedule for a given {@link Train}, travelling a given {@link Route}, experiencing no {@link MaintenanceWindow} s.
     * 
     * @param r Route to schedule.
     * @param t Train to schedule.
     */
    public Itinerary(final Route r, final Train t) {
        this(r, t, null);
    }

    /**
     * Create schedule for a given {@link Train}, travelling a given {@link Route}, possibly experiencing some
     * {@link MaintenanceWindow}s.
     * 
     * @param r Route to schedule.
     * @param t Train to schedule.
     * @param maintenanceWindows Maintenance windows to account for. Null if none.
     */
    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
        if (r == null || t == null) {
            throw new IllegalArgumentException("Neither route nor train may be null.");
        }
        if (!r.isPossibleForTrain(t)) {
            throw new IllegalArgumentException("Route " + r.getId() + " impossible for train "
                    + t.getName() + ".");
        }
        this.route = r;
        this.train = t;
        this.trainEntryTime = t.getEntryTime(Itinerary.DEFAULT_TIME_UNIT);

        final ArcProgression progression = this.getRoute().getProgression();
        Node currentNode = this.getTrain().getOrigin();
        do {
            this.hasNodes.add(currentNode);
        } while ((currentNode = progression.getNextNode(currentNode)) != this.getTrain()
                .getDestination());
        this.hasNodes.add(currentNode);

        // initialize the maintenance windows
        if (maintenanceWindows != null) {
            for (final MaintenanceWindow mow : maintenanceWindows) {
                final Node origin = mow.getOrigin(t);
                final Node destination = mow.getDestination(t);
                if (this.hasNode(origin) && this.hasNode(destination)) {
                    this.maintenances.put(origin, mow);
                }
            }
        }
    }

    private void cacheSchedule() {
        if (this.scheduleCacheValid.get()) {
            return;
        }
        SortedMap<Long, Node> scheduleCache = new TreeMap<>();
        SortedMap<Long, Arc> scheduleCacheWithArcs = new TreeMap<>();
        int i = 0;
        long previousTime = 0;
        Arc previousArc = null;
        for (final Node currentNode : this.hasNodes) {
            final Arc currentArc = this.getRoute().getProgression().getWithOriginNode(currentNode);
            long time = 0;
            if (i == 0) {
                // first item needs to be augmented by the train entry time
                time += this.trainEntryTime;
            } else {
                // otherwise we need to convert a relative time to an absolute time by adding the previous node's time
                time = this.getTrain().getArcTravellingTime(previousArc,
                        Itinerary.DEFAULT_TIME_UNIT);
                time += previousTime;
            }
            // now adjust for node wait time, should there be any
            final WaitTime wt = this.nodeWaitTimes.get(currentNode);
            if (wt != null) {
                time += wt.getWaitFor(Itinerary.DEFAULT_TIME_UNIT);
            }
            // check for maintenance windows
            if (this.maintenances.containsKey(currentNode)) {
                // there is a maintenance registered for the next node
                final MaintenanceWindow w = this.maintenances.get(currentNode);
                if (w.isInside(time, Itinerary.DEFAULT_TIME_UNIT)) { // the maintenance is ongoing, we have to wait
                    // and adjust total node entry time
                    time = w.getEnd(Itinerary.DEFAULT_TIME_UNIT);
                }
            }
            // and store
            final Long time2 = Long.valueOf(time);
            scheduleCache.put(time2, currentNode);
            scheduleCacheWithArcs.put(time2, currentArc);
            previousTime = time;
            previousArc = currentArc;
            i++;
        }
        this.scheduleCache = Collections.unmodifiableSortedMap(scheduleCache);
        this.scheduleCacheWithArcs = Collections.unmodifiableSortedMap(scheduleCacheWithArcs);
        this.scheduleCacheValid.set(true);
    }

    /**
     * Instances only equal when they share the same train, route and wait times.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Itinerary)) {
            return false;
        }
        final Itinerary other = (Itinerary) obj;
        return new EqualsBuilder().append(this.getTrain(), other.getTrain())
                .append(this.getRoute(), other.getRoute())
                .append(this.nodeWaitTimes, other.nodeWaitTimes).isEquals();
    }

    /**
     * When the train arrived at the destination.
     * 
     * @return Time in milliseconds since the start of the planning horizon when the train's lead engine first reached the
     *         destination node.
     */
    public long getArrivalTime() {
        return this.getSchedule().lastKey();
    }

    /**
     * When the train arrived at the {@link Arc}.
     * 
     * @param a The arc in question.
     * @return Time in milliseconds since the start of the planning horizon when the train's lead engine first reached given
     *         arc's origin node.
     */
    public long getArrivalTime(final Arc a) {
        if (a == null) {
            throw new IllegalArgumentException("Arc cannot be null.");
        }
        return this.getArrivalTime(a.getOrigin(this.getTrain()));
    }

    /**
     * When the train arrived at the {@link Node}.
     * 
     * @param n The node in question.
     * @return Time in milliseconds since the start of the planning horizon when the train's lead engine first reached given
     *         node.
     */
    public long getArrivalTime(final Node n) {
        if (n == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }
        final SortedMap<Long, Node> nodeEntryTimes = this.getSchedule();
        for (final SortedMap.Entry<Long, Node> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == n) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException(
                "Proper node cannot be found! Possibly a bug in the algoritm.");
    }

    /**
     * The time spent not moving, from {@link Train#getEntryTime(TimeUnit)} to the specified time.
     * 
     * @param horizon The horizon in milliseconds at which to stop counting the time.
     * @return Time in milliseconds spent waiting somewhere on the {@link Route}.
     */
    public long getDelay(final long horizon) {
        long delay = 0;
        for (final Node n : this.getRoute().getProgression().getNodes()) {
            if (!this.hasNode(n) || n == this.getTrain().getDestination()) {
                continue;
            }
            long arrivalTime = -1;
            if (n == this.getTrain().getOrigin()) {
                arrivalTime = this.getTrain().getEntryTime(TimeUnit.MILLISECONDS);
            } else {
                arrivalTime = this.getArrivalTime(n);
            }
            if (arrivalTime > horizon) {
                continue;
            }
            final long leaveTime = this.getLeaveTime(n);
            final long travellingTime = leaveTime - arrivalTime;
            final Arc currentArc = this.getRoute().getProgression().getWithOriginNode(n);
            final BigDecimal currentSpeed = this.getTrain().getMaximumSpeed(currentArc.getTrack());
            final long optimalTravellingTime = Converter.getTimeFromSpeedAndDistance(currentSpeed,
                    currentArc.getLength());
            final long difference = travellingTime - optimalTravellingTime;
            if (difference > 0) {
                // make sure the delay is never so large that it includes parts outside the planning horizon
                delay += Math.min(arrivalTime + difference, horizon) - arrivalTime;
            } else if (difference < 0) {
                throw new IllegalStateException("Delay was smaller than zero! This must be a bug.");
            }
        }
        return delay;
    }

    /**
     * Retrieve the {@link Arc} where the train is at the specified moment.
     * 
     * @param time The place in the schedule where to look at, in milliseconds.
     * @return The arc occupied by the leading engine of the {@link Train} at the given time.
     */
    protected Arc getLeadingArc(final long time) {
        if (time < this.trainEntryTime) {
            return null;
        }
        final Long time2 = Long.valueOf(time);
        final SortedMap<Long, Arc> arcs = this.getScheduleWithArcs().tailMap(time2);
        if (arcs.size() == 0) {
            return null;
        }
        final Long arcTime = arcs.firstKey();
        final Arc arc = arcs.get(arcTime);
        if (time2.compareTo(arcTime) > 0) {
            return arc;
        } else {
            return this.getRoute().getProgression().getPreviousArc(arc);
        }
    }

    /**
     * When the train left the {@link Arc}.
     * 
     * @param a The arc in question.
     * @return Time in milliseconds since the start of the planning horizon when the train's lead engine first reached given
     *         arc's destination node. -1 when there's no destination node.
     */
    public long getLeaveTime(final Arc a) {
        final Arc nextArc = this.route.getProgression().getNextArc(a);
        if (nextArc == null) {
            return -1;
        }
        return this.getArrivalTime(nextArc);
    }

    /**
     * When the train left the {@link Node}.
     * 
     * @param n The node in question.
     * @return Time in milliseconds since the start of the planning horizon when the train's lead engine first reached given
     *         nodes's next node. -1 when there's no next node.
     */
    public long getLeaveTime(final Node n) {
        final Node nextNode = this.route.getProgression().getNextNode(n);
        if (nextNode == null) {
            return -1;
        }
        return this.getArrivalTime(nextNode);
    }

    protected Map<Node, MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    public Route getRoute() {
        return this.route;
    }

    /**
     * Return the actual schedule. This is cached on access.
     * 
     * @return Map, where keys are the time of arrival and the values are the {@link Node}s arrived at.
     */
    public SortedMap<Long, Node> getSchedule() {
        this.cacheSchedule();
        return this.scheduleCache;
    }

    /**
     * Return the actual schedule. This is cached on access.
     * 
     * @return Map, where keys are the time of arrival and the values are the {@link Arc}s arrived at.
     */
    public SortedMap<Long, Arc> getScheduleWithArcs() {
        this.cacheSchedule();
        return this.scheduleCacheWithArcs;
    }

    /**
     * The time spent moving on unpreferred tracks, from {@link Train#getEntryTime(TimeUnit)} to the specified time.
     * 
     * @param time The horizon in milliseconds at which to stop counting the time.
     * @return Time in milliseconds spent on unpreferred tracks.
     */
    public long getTimeSpentOnUnpreferredTracks(final long time) {
        final SortedMap<Long, Arc> arcEntryTimes = this.getScheduleWithArcs();
        long spentTime = 0;
        final Arc leadingArc = this.getLeadingArc(time);
        /*
         * the time spent in between the nodes is calculated as a difference of their entry times; if we calculated just the
         * time spent traversing the arc, we would have missed wait times and MOWs.
         */
        long previousTimeOfEntry = 0;
        Arc previousArc = null;
        for (final SortedMap.Entry<Long, Arc> entry : arcEntryTimes.headMap(time).entrySet()) {
            final long currentTimeOfEntry = entry.getKey();
            final Arc a = entry.getValue();
            if (previousArc != null && !this.getRoute().getProgression().isPreferred(previousArc)) {
                if (previousArc == leadingArc) {
                    // include the time spent on this track so far
                    spentTime += time - currentTimeOfEntry;
                }
                // include the whole time spent on previous
                spentTime += currentTimeOfEntry - previousTimeOfEntry;
            }
            previousTimeOfEntry = currentTimeOfEntry;
            previousArc = a;
        }
        return spentTime;
    }

    public Train getTrain() {
        return this.train;
    }

    /**
     * Get time a {@link Train} is stopped for at a particular {@link Node}.
     * 
     * @param n Node to stop the train at.
     * @return The time to wait for.
     */
    public WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    /**
     * Get all times a {@link Train} is stopped at a particular {@link Node}.
     */
    public Map<Node, WaitTime> getWaitTimes() {
        return Collections.unmodifiableMap(this.nodeWaitTimes);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getTrain()).append(this.getRoute())
                .append(this.nodeWaitTimes).toHashCode();
    }

    /**
     * Whether or not the {@link Train}'s lead engine passes through the {@link Node} at some point.
     * 
     * @param n The node in question.
     * @return True if the {@link Node} is in the itinerary.
     */
    public boolean hasNode(final Node n) {
        return this.hasNodes.contains(n);
    }

    private void invalidateCaches() {
        this.scheduleCacheValid.set(false);
    }

    /**
     * Don't stop the {@link Train} at a particular {@link Node} any more.
     * 
     * @param n Node in question.
     * @return The time it originally waited there.
     */
    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            Itinerary.logger.debug("Removing wait time for {} from {}.", new Object[] { n, this });
            this.invalidateCaches();
            return this.nodeWaitTimes.remove(n);
        } else {
            Itinerary.logger.debug("No wait time to remove for {} from {}.",
                    new Object[] { n, this });
            return null;
        }
    }

    /**
     * Don't stop the {@link Train} anywhere, except for {@link MaintenanceWindow}s.
     */
    public void removeWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            Itinerary.logger.debug("Removing all wait times from {}.", new Object[] { this });
            this.invalidateCaches();
        }
        this.nodeWaitTimes.clear();
    }

    /**
     * Make the {@link Train} stop at a particular {@link Node}.
     * 
     * @param n Node to stop the train at.
     * @param w How long to wait there.
     * @return The previous wait time or null if none.
     */
    public WaitTime setWaitTime(final Node n, final WaitTime w) {
        if (w == null) {
            return this.removeWaitTime(n);
        }
        this.invalidateCaches();
        final WaitTime previous = this.nodeWaitTimes.put(n, w);
        Itinerary.logger.debug("Set {} on {} in {}, replacing {}.", new Object[] { w, n, this,
                previous });
        return previous;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Itinerary [route=").append(this.route.getId()).append(", train=")
                .append(this.train.getName()).append("]");
        return builder.toString();
    }

    @Override
    public boolean visualize(final File target) {
        return this.visualize(target, -1);
    }

    public boolean visualize(final File target, final long time) {
        return this.visualize(new ItineraryVisualizer(this), target);
    }
}
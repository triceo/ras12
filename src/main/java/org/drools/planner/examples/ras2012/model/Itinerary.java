package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.planner.examples.ras2012.Visualizable;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.model.ArcProgression;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker;
import org.drools.planner.examples.ras2012.util.visualizer.ItineraryVisualizer;

public final class Itinerary extends Visualizable {

    private static final TimeUnit              DEFAULT_TIME_UNIT     = TimeUnit.MILLISECONDS;

    private final Route                        route;

    private final Train                        train;

    private final AtomicBoolean                scheduleCacheValid    = new AtomicBoolean(false);
    private final Collection<Node>             nodesEnRoute;
    private final SortedMap<Long, Node>        scheduleCache         = new TreeMap<Long, Node>();
    private final SortedMap<Long, Arc>         scheduleCacheWithArcs = new TreeMap<Long, Arc>();
    private final long                         trainEntryTime;
    private final Map<Node, WaitTime>          nodeWaitTimes         = new HashMap<Node, WaitTime>();

    // FIXME only one window per node; multiple different windows with same node will get lost
    private final Map<Node, MaintenanceWindow> maintenances          = new HashMap<Node, MaintenanceWindow>();

    public Itinerary(final Route r, final Train t) {
        this(r, t, null);
    }

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
        this.nodesEnRoute = this.getRoute().getProgression().tail(this.getTrain().getOrigin())
                .getNodes();
        // initialize the maintenance windows
        if (maintenanceWindows != null) {
            for (final MaintenanceWindow mow : maintenanceWindows) {
                final Node origin = mow.getOrigin(t);
                final Node destination = mow.getDestination(t);
                if (this.isNodeOnRoute(origin) && this.isNodeOnRoute(destination)) {
                    this.maintenances.put(origin, mow);
                }
            }
        }
    }

    private void cacheSchedule() {
        int i = 0;
        long previousTime = 0;
        Arc previousArc = null;
        boolean seekingStart = true;
        for (final Arc currentArc : this.getRoute().getProgression().getArcs()) {
            // arc progression begins with the start of the route; the train doesn't necessarily start there
            if (seekingStart) {
                if (currentArc.getOrigin(this.getTrain()) != this.getTrain().getOrigin()) {
                    continue;
                } else {
                    seekingStart = false;
                }
            }
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
            final Node n = currentArc.getOrigin(this.getTrain());
            final WaitTime wt = this.nodeWaitTimes.get(n);
            if (wt != null) {
                time += wt.getWaitFor(Itinerary.DEFAULT_TIME_UNIT);
            }
            // check for maintenance windows
            if (this.maintenances.containsKey(n)) {
                // there is a maintenance registered for the next node
                final MaintenanceWindow w = this.maintenances.get(n);
                if (w.isInside(time, Itinerary.DEFAULT_TIME_UNIT)) { // the maintenance is ongoing, we have to wait
                    // and adjust total node entry time
                    time = w.getEnd(Itinerary.DEFAULT_TIME_UNIT);
                }
            }
            // and store
            this.scheduleCache.put(time, n);
            this.scheduleCacheWithArcs.put(time + 1, currentArc);
            previousTime = time;
            previousArc = currentArc;
            i++;
        }
        final long time = previousTime
                + this.getTrain().getArcTravellingTime(previousArc, Itinerary.DEFAULT_TIME_UNIT);
        this.scheduleCache.put(time, previousArc.getDestination(this.getTrain()));
        this.scheduleCacheWithArcs.put(time + 1, null);
        this.scheduleCacheValid.set(true);
    }

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
        if (this.train != other.train) {
            return false;
        }
        if (this.route != other.route) {
            return false;
        }
        if (!this.nodeWaitTimes.equals(other.nodeWaitTimes)) {
            return false;
        }
        return true;
    }

    public long getArrivalTime() {
        return this.getSchedule().lastKey();
    }

    /**
     * Return the delay at the end of the horizon.
     * 
     * @param horizon Number of milliseconds since the start of the planning horizon.
     * @return Milliseconds. Positive if the train is late, negative if the train is early. If the train arrived before the
     *         specified horizon, this returns the exact value of delay. Otherwise, it returns estimated delay at the end of the
     *         specified horizon.
     */
    public long getDelay(final long horizon) {
        if (this.trainEntryTime > horizon) {
            // train not en route yet, delay must be zero
            return 0;
        }
        final long originalDelay = this.getTrain().getOriginalSA(Itinerary.DEFAULT_TIME_UNIT);
        final SortedMap<Long, Node> schedule = this.getSchedule();
        final SortedMap<Long, Node> scheduleInHorizon = schedule.headMap(horizon + 1);
        if (scheduleInHorizon.size() == 0) {
            // train is not in the network at the time
            if (this.trainEntryTime < horizon) {
                // however, it should be; origin node possibly has some wait time
                return horizon - this.trainEntryTime + originalDelay;
            } else {
                // otherwise it's alright, there is only the original delay
                return originalDelay;
            }
        } else if (scheduleInHorizon.values().contains(this.getTrain().getDestination())) {
            return this.getDelay(this.getTrain().getDestination(),
                    this.getTrain().getWantTime(Itinerary.DEFAULT_TIME_UNIT));
        }
        // otherwise, we need to estimate the delay
        final long actualArrivalTime = horizon;
        final long optimalArrivalTime = this.trainEntryTime
                + Converter.getTimeFromSpeedAndDistance(this.getTrain().getMaximumSpeed(),
                        Converter.getDistanceTravelled(this, horizon));
        return actualArrivalTime - optimalArrivalTime;
    }

    /**
     * Get delay at a given node, provided we know when we were supposed to be there.
     * 
     * @param n The node on the route.
     * @param expectedArrival Time in milliseconds of the expected arrival.
     * @return Milliseconds. Positive when delayed, negative when ahead. Returns the actual delay regardless of whether the node
     *         is or isn't within the planning horizon.
     */
    private long getDelay(final Node n, final long expectedArrival) {
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            if (entry.getValue() != n) {
                continue;
            }
            return entry.getKey() - expectedArrival;
        }
        throw new IllegalArgumentException(n + " not on the route!");
    }

    private long getEntryTime(final Arc a) {
        final SortedMap<Long, Arc> nodeEntryTimes = this.getScheduleWithArcs();
        long timeEntered = -1;
        for (final SortedMap.Entry<Long, Arc> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == a) {
                timeEntered = entry.getKey() - 1;
                break;
            }
        }
        if (timeEntered < this.trainEntryTime) {
            throw new IllegalStateException(
                    "Proper arc cannot be found! Possibly a bug in the algoritm.");
        }
        return timeEntered;
    }

    private long getEntryTime(final Node n) {
        final SortedMap<Long, Node> nodeEntryTimes = this.getSchedule();
        long timeEntered = -1;
        for (final SortedMap.Entry<Long, Node> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == n) {
                timeEntered = entry.getKey();
                break;
            }
        }
        if (timeEntered < this.trainEntryTime) {
            throw new IllegalStateException(
                    "Proper node cannot be found! Possibly a bug in the algoritm.");
        }
        return timeEntered;
    }

    protected Arc getLeadingArc(final long time) {
        if (time < this.trainEntryTime) {
            return null;
        }
        final SortedMap<Long, Arc> arcs = this.getScheduleWithArcs().tailMap(time);
        if (arcs.size() == 0) {
            return null;
        } else if (time == arcs.firstKey()) {
            return arcs.get(arcs.firstKey());
        } else {
            return this.getRoute().getProgression().getPrevious(arcs.get(arcs.firstKey()));
        }
    }

    public Map<Node, MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    public OccupationTracker getOccupiedArcs(final long time) {
        final boolean trainStarted = time <= this.getSchedule().firstKey();
        final boolean trainInOrigin = this.getTrain().getOrigin() == this.getRoute()
                .getProgression().getOrigin().getOrigin(this.getRoute());
        if (trainStarted && trainInOrigin) {
            // train not yet on the route
            return OccupationTracker.Builder.empty();
        }
        final ArcProgression progression = this.getRoute().getProgression();
        final Arc leadingArc = this.getLeadingArc(time);
        if (leadingArc == null) {
            // the train should gradually leave the network through its destination
            final long timeTravelledInArc = time - this.getSchedule().lastKey();
            final BigDecimal travelledInArc = Converter.getDistanceFromSpeedAndTime(this.getTrain()
                    .getMaximumSpeed(Track.MAIN_0), timeTravelledInArc);
            if (travelledInArc.compareTo(this.getTrain().getLength()) >= 0) {
                // the train is gone completely
                return OccupationTracker.Builder.empty();
            } else {
                // some part of the train is still in the network
                return progression.getOccupiedArcs(progression.getLength(), this.getTrain()
                        .getLength().subtract(travelledInArc));
            }
        } else if (!this.getScheduleWithArcs().containsValue(leadingArc)) {
            // the train didn't enter the network yet
            return progression.getOccupiedArcs(progression.getDistance(leadingArc
                    .getDestination(progression)), this.getTrain().getLength());
        } else {
            // the train is in the network
            final long timeTravelledInArc = time - this.getEntryTime(leadingArc);
            final BigDecimal travelledInArc = Converter.getDistanceFromSpeedAndTime(
                    this.getTrain().getMaximumSpeed(leadingArc.getTrack()), timeTravelledInArc)
                    .min(leadingArc.getLength());
            return progression.getOccupiedArcs(
                    progression.getDistance(leadingArc.getOrigin(progression)).add(travelledInArc),
                    this.getTrain().getLength());
        }
    }

    public Route getRoute() {
        return this.route;
    }

    public synchronized SortedMap<Long, Node> getSchedule() {
        if (!this.scheduleCacheValid.get() || this.scheduleCache.size() == 0) {
            this.cacheSchedule();
        }
        return Collections.unmodifiableSortedMap(this.scheduleCache);
    }

    public Map<Node, Long> getArrivalsAtSANodes() {
        final Map<Node, Long> result = new HashMap<Node, Long>();
        for (final ScheduleAdherenceRequirement sa : this.getTrain()
                .getScheduleAdherenceRequirements().values()) {
            final Node expectedDestination = sa.getDestination();
            result.put(expectedDestination, this.getEntryTime(expectedDestination));
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized SortedMap<Long, Arc> getScheduleWithArcs() {
        if (!this.scheduleCacheValid.get() || this.scheduleCache.size() == 0) {
            this.cacheSchedule();
        }
        return Collections.unmodifiableSortedMap(this.scheduleCacheWithArcs);
    }

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

    public synchronized WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    public Map<Node, WaitTime> getWaitTimes() {
        return Collections.unmodifiableMap(this.nodeWaitTimes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.nodeWaitTimes.hashCode();
        result = prime * result + this.route.hashCode();
        result = prime * result + this.train.hashCode();
        return result;
    }

    private synchronized void invalidateCaches() {
        this.scheduleCache.clear();
        this.scheduleCacheWithArcs.clear();
    }

    public boolean isNodeOnRoute(final Node n) {
        return this.nodesEnRoute.contains(n);
    }

    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            this.invalidateCaches();
            return this.nodeWaitTimes.remove(n);
        } else {
            return null;
        }
    }

    public void removeWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            this.invalidateCaches();
        }
        this.nodeWaitTimes.clear();
    }

    public synchronized WaitTime setWaitTime(final Node n, final WaitTime w) {
        if (!this.getRoute().getProgression().getWaitPoints().contains(n)) {
            throw new IllegalArgumentException(n + " not a wait point: " + this);
        }
        if (w == null) {
            return this.removeWaitTime(n);
        }
        this.invalidateCaches();
        final WaitTime previous = this.nodeWaitTimes.get(n);
        this.nodeWaitTimes.put(n, w);
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
        return this.visualize(new ItineraryVisualizer(this, time), target);
    }
}
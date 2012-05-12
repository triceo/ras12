package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.planner.examples.ras2012.interfaces.Visualizable;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.ScheduleAdherenceRequirement;
import org.drools.planner.examples.ras2012.model.original.Track;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.model.original.WaitTime;
import org.drools.planner.examples.ras2012.util.ArcProgression;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.ItineraryVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Itinerary implements Visualizable {

    private static final TimeUnit              DEFAULT_TIME_UNIT     = TimeUnit.MILLISECONDS;

    private static final Logger                logger                = LoggerFactory
                                                                             .getLogger(Itinerary.class);

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

    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
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
        for (final MaintenanceWindow mow : maintenanceWindows) {
            final Node n = mow.getOrigin(t);
            this.maintenances.put(n, mow);
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
        if (previousArc == null) {
            throw new IllegalStateException("previousArc == null. That shouldn't have happened!");
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
        if (this.nodeWaitTimes == null) {
            if (other.nodeWaitTimes != null) {
                return false;
            }
        } else if (!this.nodeWaitTimes.equals(other.nodeWaitTimes)) {
            return false;
        }
        if (this.route == null) {
            if (other.route != null) {
                return false;
            }
        } else if (!this.route.equals(other.route)) {
            return false;
        }
        if (this.train == null) {
            if (other.train != null) {
                return false;
            }
        } else if (!this.train.equals(other.train)) {
            return false;
        }
        return true;
    }

    public Map<Node, WaitTime> getAllWaitTimes() {
        return Collections.unmodifiableMap(this.nodeWaitTimes);
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
        final long originalDelay = this.getTrain().getOriginalDelay(Itinerary.DEFAULT_TIME_UNIT);
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
                        Converter.calculateActualDistanceTravelled(this, horizon));
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
        long timeArcEntered = -1;
        for (final SortedMap.Entry<Long, Arc> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == a) {
                timeArcEntered = entry.getKey() - 1;
                break;
            }
        }
        if (timeArcEntered < this.trainEntryTime) {
            throw new IllegalStateException(
                    "Proper arc cannot be found! Possibly a bug in the algoritm.");
        }
        return timeArcEntered;
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

    public Collection<Arc> getOccupiedArcs(final long time) {
        if (time <= this.getSchedule().firstKey()) {
            return Collections.emptySet();
        }
        final ArcProgression progression = this.getRoute().getProgression();
        final Arc leadingArc = this.getLeadingArc(time);
        if (leadingArc == null) {
            // the train should gradually leave the network through its destination
            final long timeTravelledInArc = time - this.getEntryTime(progression.getPrevious(null));
            final BigDecimal travelledInArc = Converter.getDistanceInMilesFromSpeedAndTime(this
                    .getTrain().getMaximumSpeed(Track.MAIN_0), timeTravelledInArc);
            if (travelledInArc.compareTo(this.getTrain().getLengthInMiles()) >= 0) {
                // the train is gone completely
                return Collections.emptySet();
            } else {
                // some part of the train is still in the network
                return progression.getOccupiedArcs(progression.getLength(), this.getTrain()
                        .getLengthInMiles().subtract(travelledInArc));
            }
        } else if (!this.getScheduleWithArcs().containsValue(leadingArc)) {
            // the train didn't enter the network yet
            return progression.getOccupiedArcs(progression.getDistance(leadingArc
                    .getDestination(progression)), this.getTrain().getLengthInMiles());
        } else {
            // the train is in the network
            final long timeTravelledInArc = time - this.getEntryTime(leadingArc);
            final BigDecimal travelledInArc = Converter.getDistanceInMilesFromSpeedAndTime(
                    this.getTrain().getMaximumSpeed(leadingArc.getTrack()), timeTravelledInArc)
                    .max(leadingArc.getLengthInMiles());
            return progression.getOccupiedArcs(
                    progression.getDistance(leadingArc.getOrigin(progression)).add(travelledInArc),
                    this.getTrain().getLengthInMiles());
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

    public Map<Node, Long> getScheduleAdherenceStatus() {
        final Map<Node, Long> result = new HashMap<Node, Long>();
        for (final ScheduleAdherenceRequirement sa : this.getTrain()
                .getScheduleAdherenceRequirements()) {
            final Node expectedDestination = sa.getDestination();
            final long expectedArrival = sa.getTimeSinceStartOfWorld(Itinerary.DEFAULT_TIME_UNIT);
            result.put(expectedDestination, this.getDelay(expectedDestination, expectedArrival));
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

    public long getWantTimeDifference() {
        return this.getDelay(this.getTrain().getDestination(),
                this.getTrain().getWantTime(Itinerary.DEFAULT_TIME_UNIT));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.nodeWaitTimes == null ? 0 : this.nodeWaitTimes.hashCode());
        result = prime * result + (this.route == null ? 0 : this.route.hashCode());
        result = prime * result + (this.train == null ? 0 : this.train.hashCode());
        return result;
    }

    private synchronized void invalidateCaches() {
        this.scheduleCache.clear();
        this.scheduleCacheWithArcs.clear();
    }

    public boolean isNodeOnRoute(final Node n) {
        return this.nodesEnRoute.contains(n);
    }

    public void removeAllWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            this.invalidateCaches();
        }
        this.nodeWaitTimes.clear();
    }

    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            this.invalidateCaches();
            return this.nodeWaitTimes.remove(n);
        } else {
            return null;
        }
    }

    public synchronized WaitTime setWaitTime(final WaitTime w, final Node n) {
        if (w == null) {
            return this.removeWaitTime(n);
        }
        if (!this.isNodeOnRoute(n)) {
            throw new IllegalArgumentException(n + " not in the itinerary: " + this);
        }
        if (this.getRoute().getProgression().getWaitPoints().contains(n)) {
            this.invalidateCaches();
            final WaitTime previous = this.nodeWaitTimes.get(n);
            this.nodeWaitTimes.put(n, w);
            return previous;
        } else {
            return null;
        }
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
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            Itinerary.logger.info("Starting visualizing itinerary: " + this);
            new ItineraryVisualizer(this, time).visualize(os);
            Itinerary.logger.info("Itinerary visualization finished: " + this);
            return true;
        } catch (final Exception ex) {
            Itinerary.logger.error("Visualizing itinerary " + this + " failed.", ex);
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
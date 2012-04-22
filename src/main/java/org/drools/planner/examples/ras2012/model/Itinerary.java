package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;
import org.drools.planner.examples.ras2012.util.ItineraryVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Itinerary implements ScheduleProducer {

    private static final Logger logger = LoggerFactory.getLogger(Itinerary.class);

    private static BigDecimal getDistanceInMilesFromSpeedAndTime(final int speedInMPH,
            final long timeInMilliseconds) {
        final BigDecimal timeInSeconds = BigDecimal.valueOf(timeInMilliseconds).divide(
                BigDecimal.valueOf(1000), 10, BigDecimal.ROUND_HALF_EVEN);
        final BigDecimal timeInMinutes = timeInSeconds.divide(BigDecimal.valueOf(60), 10,
                BigDecimal.ROUND_HALF_EVEN);
        final BigDecimal milesPerHour = BigDecimal.valueOf(speedInMPH);
        final BigDecimal milesPerMinute = milesPerHour.divide(BigDecimal.valueOf(60), 10,
                BigDecimal.ROUND_HALF_EVEN);
        return milesPerMinute.multiply(timeInMinutes);
    }

    private final Route                        route;

    private final Train                        train;

    private final AtomicBoolean                scheduleCacheValid = new AtomicBoolean(false);

    private final Set<Node>                    nodesEnRoute       = new HashSet<Node>();
    private final SortedMap<Long, Node>        scheduleCache      = new TreeMap<Long, Node>();
    private final Map<Node, Long>              delayCache         = new HashMap<Node, Long>();
    private final long                         trainEntryTime;
    private final List<Arc>                    arcProgression     = new LinkedList<Arc>();
    private final Map<Node, Arc>               arcPerStartNode    = new HashMap<Node, Arc>();
    private final Map<Node, WaitTime>          nodeWaitTimes      = new HashMap<Node, WaitTime>();
    // FIXME only one window per node; multiple different windows with same node will get lost
    private final Map<Node, MaintenanceWindow> maintenances       = new HashMap<Node, MaintenanceWindow>();

    private final Map<Long, Collection<Arc>>   occupiedArcsCache  = new HashMap<Long, Collection<Arc>>();

    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
        if (!r.isPossibleForTrain(t)) {
            throw new IllegalArgumentException("Route " + r.getId() + " impossible for train "
                    + t.getName() + ".");
        }
        this.route = r;
        this.train = t;
        this.trainEntryTime = TimeUnit.MINUTES.toMillis(t.getEntryTime());
        // assemble the node-traversal information
        boolean journeyOngoing = false;
        Arc currentArc = null;
        while ((currentArc = this.route.getNextArc(currentArc)) != null) {
            if (currentArc.getStartingNode(this.train) == this.train.getOrigin()) {
                journeyOngoing = true;
            }
            this.arcProgression.add(currentArc);
            this.arcPerStartNode.put(currentArc.getStartingNode(this.getRoute()), currentArc);
            if (journeyOngoing) {
                this.nodesEnRoute.add(currentArc.getEastNode());
                this.nodesEnRoute.add(currentArc.getWestNode());
            }
            if (currentArc.getEndingNode(this.train) == this.train.getDestination()) {
                journeyOngoing = false;
            }
        }
        // initialize the maintenance windows
        for (final MaintenanceWindow mow : maintenanceWindows) {
            final Node n = t.isEastbound() ? mow.getWestNode() : mow.getEastNode();
            this.maintenances.put(n, mow);
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

    @Override
    public Map<Node, WaitTime> getAllWaitTimes() {
        return Collections.unmodifiableMap(this.nodeWaitTimes);
    }

    private Arc getArcPerStartingNode(final Node n) {
        return this.arcPerStartNode.get(n);
    }

    @Override
    public Collection<Arc> getCurrentlyOccupiedArcs(final long time) {
        if (!this.occupiedArcsCache.containsKey(time)) {
            final Collection<Arc> a = this.getCurrentlyOccupiedArcsUncached(time);
            this.occupiedArcsCache.put(time, a);
            return a;
        }
        return this.occupiedArcsCache.get(time);
    }

    private Collection<Arc> getCurrentlyOccupiedArcsUncached(final long time) {
        final Arc leadingArc = this.getLeadingArc(time);
        if (leadingArc == null) {
            // train not in the network
            // FIXME train should leave the network gradually, not at once when it reaches destination
            return new HashSet<Arc>();
        }
        final List<Arc> occupiedArcs = new LinkedList<Arc>();
        occupiedArcs.add(leadingArc);
        // calculate how far are we into the leading arc
        final SortedMap<Long, Arc> nodeEntryTimes = this.getScheduleWithArcs();
        long timeArcEntered = -1;
        for (final SortedMap.Entry<Long, Arc> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == leadingArc) {
                timeArcEntered = entry.getKey();
                break;
            }
        }
        if (timeArcEntered < TimeUnit.MINUTES.toMillis(this.getTrain().getEntryTime())) {
            throw new IllegalStateException(
                    "Proper arc cannot be found! Possibly a bug in the algoritm.");
        }
        final long timeTravelledInLeadingArc = time - timeArcEntered;
        final BigDecimal travelledInLeadingArc = Itinerary.getDistanceInMilesFromSpeedAndTime(this
                .getTrain().getMaximumSpeed(leadingArc.getTrackType()), timeTravelledInLeadingArc);
        BigDecimal remainingLengthOfTrain = this.getTrain().getLength()
                .subtract(travelledInLeadingArc);
        // and now add any preceding arcs for as long as the remaining train length > 0
        Arc currentlyProcessedArc = leadingArc;
        while ((currentlyProcessedArc = this.getRoute().getPreviousArc(currentlyProcessedArc)) != null) {
            occupiedArcs.add(currentlyProcessedArc);
            remainingLengthOfTrain = remainingLengthOfTrain.subtract(currentlyProcessedArc
                    .getLengthInMiles());
            if (remainingLengthOfTrain.compareTo(BigDecimal.ZERO) < 0) {
                break;
            }
        }
        return occupiedArcs;
    }

    @Override
    public synchronized Map<Node, Long> getDelays() {
        // FIXME make this work based on actual delays (actual arrival times vs. straight non-stop max-speed arrival times)
        this.getSchedule(); // just to make sure the delay cache is fresh
        return Collections.unmodifiableMap(this.delayCache);
    }

    protected Arc getLeadingArc(final long time) {
        if (time < this.trainEntryTime) {
            return null;
        }
        Node previousNode = null;
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            final long timeOfArrival = entry.getKey();
            final Node currentNode = entry.getValue();
            if (time > timeOfArrival) {
                previousNode = currentNode;
                continue;
            } else if (time == timeOfArrival) {
                return this.getArcPerStartingNode(currentNode);
            } else {
                return this.getArcPerStartingNode(previousNode);
            }
        }
        return null;
    }

    public Map<Node, MaintenanceWindow> getMaintenances() {
        return this.maintenances;
    }

    @Override
    public Route getRoute() {
        return this.route;
    }

    @Override
    public synchronized SortedMap<Long, Node> getSchedule() {
        if (!this.scheduleCacheValid.get() || this.scheduleCache.size() == 0) {
            int i = 0;
            long previousTime = 0;
            Arc previousArc = null;
            boolean seekingStart = true;
            for (final Arc currentArc : this.arcProgression) {
                // arc progression begins with the start of the route; the train doesn't necessarily start there
                if (seekingStart) {
                    if (currentArc.getStartingNode(this.getTrain()) != this.getTrain().getOrigin()) {
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
                    time = this.getTrain().getArcTravellingTimeInMilliseconds(previousArc);
                    time += previousTime;
                }
                // now adjust for node wait time, should there be any
                final Node n = currentArc.getStartingNode(this.getTrain());
                final WaitTime wt = this.nodeWaitTimes.get(n);
                if (wt != null) {
                    this.delayCache.put(n, wt.getMillisWaitFor());
                    time += wt.getMillisWaitFor();
                }
                // check for maintenance windows
                if (this.maintenances.containsKey(n)) {
                    // there is a maintenance registered for the next node
                    final MaintenanceWindow w = this.maintenances.get(n);
                    if (w.isInside(time)) { // the maintenance is ongoing, we have to wait
                        // make sure the delay cache has been updated
                        long difference = w.getEnd() - time;
                        if (this.delayCache.containsKey(n)) {
                            difference += this.delayCache.get(n);
                        }
                        this.delayCache.put(n, difference);
                        // and adjust total node entry time
                        time = w.getEnd();
                    }
                }
                // and store
                this.scheduleCache.put(time, n);
                previousTime = time;
                previousArc = currentArc;
                i++;
            }
            this.scheduleCache.put(previousTime
                    + this.getTrain().getArcTravellingTimeInMilliseconds(previousArc),
                    previousArc.getEndingNode(this.getTrain()));
            this.scheduleCacheValid.set(true);
        }
        return Collections.unmodifiableSortedMap(this.scheduleCache);
    }

    @Override
    public Map<Long, Long> getScheduleAdherenceStatus() {
        // FIXME make this work based on actual delays (actual arrival times vs. straight non-stop max-speed arrival times)
        final Map<Long, Long> result = new HashMap<Long, Long>();
        for (final ScheduleAdherenceRequirement sa : this.getTrain()
                .getScheduleAdherenceRequirements()) {
            final Node pointOnRoute = sa.getDestination();
            final long expectedTime = TimeUnit.MINUTES.toMillis(sa.getTimeSinceStartOfWorld());
            for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
                if (entry.getValue() == pointOnRoute) {
                    // make sure we only include the time within the planning horizon
                    final long actualTime = Math.min(entry.getKey(),
                            TimeUnit.MINUTES.toMillis(RAS2012Solution.PLANNING_HORIZON_MINUTES));
                    final long difference = actualTime - expectedTime;
                    result.put(entry.getKey(), difference);
                }
            }
        }
        if (result.size() != this.getTrain().getScheduleAdherenceRequirements().size()) {
            throw new IllegalStateException("Failed acquiring SA status!");
        }
        return result;
    }

    @Override
    public SortedMap<Long, Arc> getScheduleWithArcs() {
        final SortedMap<Long, Arc> entries = new TreeMap<Long, Arc>();
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            entries.put(entry.getKey(), this.getArcPerStartingNode(entry.getValue()));
        }
        return Collections.unmodifiableSortedMap(entries);
    }

    @Override
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
        for (final SortedMap.Entry<Long, Arc> entry : arcEntryTimes.entrySet()) {
            final long currentTimeOfEntry = entry.getKey();
            if (currentTimeOfEntry > time) {
                // we're not interested in values that are beyond the specified time
                continue;
            }
            final Arc a = entry.getValue();
            if (previousArc != null && !this.getRoute().isArcPreferred(previousArc)) {
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

    @Override
    public Train getTrain() {
        return this.train;
    }

    @Override
    public synchronized WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    @Override
    public Map<Long, Long> getWantTimeDifference() {
        final Map<Long, Long> result = new HashMap<Long, Long>();
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            if (entry.getValue() == this.getTrain().getDestination()) {
                // make sure we only include the time within the planning horizon
                final long actualTime = Math.min(entry.getKey(),
                        TimeUnit.MINUTES.toMillis(RAS2012Solution.PLANNING_HORIZON_MINUTES));
                final long difference = actualTime
                        - TimeUnit.MINUTES.toMillis(this.getTrain().getWantTime());
                result.put(entry.getKey(), difference);
            }
        }
        return result;
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
        this.occupiedArcsCache.clear();
        this.scheduleCache.clear();
        this.delayCache.clear();
    }

    @Override
    public boolean isNodeOnRoute(final Node n) {
        return this.nodesEnRoute.contains(n);
    }

    @Override
    public void removeAllWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            this.invalidateCaches();
        }
        this.nodeWaitTimes.clear();
    }

    @Override
    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            this.invalidateCaches();
            return this.nodeWaitTimes.remove(n);
        } else {
            return null;
        }
    }

    @Override
    public synchronized WaitTime setWaitTime(final WaitTime w, final Node n) {
        if (w == null) {
            return this.removeWaitTime(n);
        }
        if (!this.isNodeOnRoute(n)) {
            throw new IllegalArgumentException(n + " not in the itinerary: " + this);
        }
        if (this.getRoute().getWaitPoints().contains(n)) {
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
        final StringBuilder sb = new StringBuilder();
        sb.append("Itinerary (");
        sb.append(this.getRoute().getLengthInMiles());
        sb.append(" miles): ");
        for (final SortedMap.Entry<Long, Node> a : this.getSchedule().entrySet()) {
            sb.append(a.getValue().getId());
            sb.append("@");
            sb.append(a.getKey());
            sb.append(" ");
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public boolean visualize(final File target) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            Itinerary.logger.info("Starting visualizing itinerary: " + this);
            new ItineraryVisualizer(this).visualize(os);
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
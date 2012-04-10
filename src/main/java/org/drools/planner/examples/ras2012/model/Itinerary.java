package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;

public final class Itinerary implements ScheduleProducer {

    public static final class Window {

        private final long start, end;

        public Window(final int start, final int end) {
            this.start = start * 1000;
            this.end = start * 1000;
        }

        /**
         * Get end of this window.
         * 
         * @return Value in milliseconds since the beginning of time.
         */
        public long getEnd() {
            return this.end;
        }

        /**
         * Whether or not the give time is inside the window.
         * 
         * @param time Time in milliseconds.
         * @return
         */
        public boolean isInside(final long time) {
            if (start > time) {
                return false; // window didn't start yet
            }
            if (end < time) {
                return false; // window is already over
            }
            return true;
        }

    }

    private static BigDecimal getDistanceInMilesFromSpeedAndTime(final int speedInMPH,
            final BigDecimal time) {
        final BigDecimal milesPerHour = BigDecimal.valueOf(speedInMPH);
        final BigDecimal milesPerMinute = milesPerHour.divide(BigDecimal.valueOf(60), 10,
                BigDecimal.ROUND_HALF_EVEN);
        return milesPerMinute.multiply(time);
    }

    private final Route                       route;

    private final Train                       train;

    private final AtomicBoolean               scheduleCacheValid = new AtomicBoolean(false);

    private final SortedMap<Long, Node>       scheduleCache      = new TreeMap<Long, Node>();

    private final long                        trainEntryTime;
    private final List<Arc>                   arcProgression     = new LinkedList<Arc>();
    private final Map<Node, Arc>              arcPerStartNode    = new HashMap<Node, Arc>();
    private final Map<Node, WaitTime>         nodeWaitTimes      = new HashMap<Node, WaitTime>();
    // FIXME only one window per node; multiple different windows with same node will get lost
    private final Map<Node, Itinerary.Window> maintenances       = new HashMap<Node, Itinerary.Window>();
    private final Map<Long, Collection<Arc>>  occupiedArcsCache  = new HashMap<Long, Collection<Arc>>();

    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
        this.route = r;
        this.train = t;
        this.trainEntryTime = t.getEntryTime() * 1000;
        // assemble the node-traversal information
        Arc currentArc = null;
        while ((currentArc = this.route.getNextArc(currentArc)) != null) {
            this.arcProgression.add(currentArc);
            this.arcPerStartNode.put(currentArc.getStartingNode(this.getRoute()), currentArc);
        }
        // initialize the maintenance windows
        for (final MaintenanceWindow mow : maintenanceWindows) {
            final Node n = t.isEastbound() ? mow.getWestNode() : mow.getEastNode();
            final Itinerary.Window w = new Window(mow.getStartingMinute(), mow.getEndingMinute());
            this.maintenances.put(n, w);
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
        final BigDecimal timeInMinutes = convertNewValueToOld(time);
        final Arc leadingArc = this.getLeadingArc(timeInMinutes);
        if (leadingArc == null) {
            // train not in the network
            // FIXME train should leave the network gradually, not at once when it reaches destination
            return new HashSet<Arc>();
        }
        final List<Arc> occupiedArcs = new LinkedList<Arc>();
        occupiedArcs.add(leadingArc);
        // calculate how far are we into the leading arc
        final Node beginningOfLeadingArc = leadingArc.getStartingNode(this.getTrain());
        final SortedMap<Long, Node> nodeEntryTimes = this.getSchedule();
        BigDecimal timeArcEntered = null;
        for (final SortedMap.Entry<Long, Node> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == beginningOfLeadingArc) {
                timeArcEntered = convertNewValueToOld(entry.getKey());
                break;
            }
        }
        final BigDecimal timeTravelledInLeadingArc = timeInMinutes.subtract(timeArcEntered);
        final BigDecimal travelledInLeadingArc = Itinerary.getDistanceInMilesFromSpeedAndTime(this
                .getTrain().getMaximumSpeed(leadingArc.getTrackType()), timeTravelledInLeadingArc);
        BigDecimal remainingLengthOfTrain = this.getTrain().getLength()
                .subtract(travelledInLeadingArc);
        // and now add any preceding arcs for as long as the remaining train length > 0
        Arc currentlyProcessedArc = leadingArc;
        while (remainingLengthOfTrain.compareTo(BigDecimal.ZERO) > 0) {
            Arc previousArc = this.getRoute().getInitialArc();
            for (final Arc a : this.arcProgression) {
                if (a == currentlyProcessedArc) {
                    break;
                } else {
                    previousArc = a;
                }
            }
            if (!occupiedArcs.contains(previousArc)) { // each arc only once
                occupiedArcs.add(previousArc);
            }
            remainingLengthOfTrain = remainingLengthOfTrain
                    .subtract(previousArc.getLengthInMiles());
            currentlyProcessedArc = previousArc;
        }
        return occupiedArcs;
    }

    protected Arc getLeadingArc(final BigDecimal timeInMinutes) {
        if (timeInMinutes.compareTo(convertNewValueToOld(this.trainEntryTime)) == -1) {
            return null;
        }
        Node previousNode = null;
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            final int comparison = timeInMinutes.compareTo(convertNewValueToOld(entry.getKey()));
            final Node currentNode = entry.getValue();
            if (comparison > 0) {
                previousNode = currentNode;
                continue;
            } else if (comparison == 0) {
                return this.getArcPerStartingNode(currentNode);
            } else {
                return this.getArcPerStartingNode(previousNode);
            }
        }
        return null;
    }

    public Map<Node, Itinerary.Window> getMaintenances() {
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
            BigDecimal previousTime = BigDecimal.ZERO;
            Arc previousArc = null;
            for (final Arc currentArc : this.arcProgression) {
                BigDecimal time = BigDecimal.ZERO;
                if (i == 0) {
                    // first item needs to be augmented by the train entry time
                    time = time.add(convertNewValueToOld(this.trainEntryTime));
                } else {
                    // otherwise we need to convert a relative time to an absolute time by adding the previous node's time
                    time = previousArc.getTravellingTimeInMinutes(this.getTrain());
                    time = time.add(previousTime);
                }
                // now adjust for node wait time, should there be any
                final Node n = currentArc.getStartingNode(this.getTrain());
                final WaitTime wt = this.nodeWaitTimes.get(n);
                if (wt != null) {
                    time = time.add(BigDecimal.valueOf(wt.getMinutesWaitFor()));
                }
                // check for maintenance windows
                if (this.maintenances.containsKey(n)) {
                    // there is a maintenance registered for the next node
                    final Itinerary.Window w = this.maintenances.get(n);
                    if (w.isInside(convertOldValueToNew(time))) {
                        // the maintenance is ongoing, we have to wait
                        time = convertNewValueToOld(w.getEnd());
                    }
                }
                // and store
                this.scheduleCache.put(convertOldValueToNew(time), n);
                previousTime = time;
                previousArc = currentArc;
                i++;
            }
            this.scheduleCache.put(convertOldValueToNew(previousTime.add(previousArc
                    .getTravellingTimeInMinutes(this.getTrain()))), previousArc.getEndingNode(this
                    .getTrain()));
            this.scheduleCacheValid.set(true);
        }
        return Collections.unmodifiableSortedMap(this.scheduleCache);
    }

    @Override
    public Map<Long, Long> getScheduleAdherenceStatus() {
        final Map<Long, Long> result = new HashMap<Long, Long>();
        for (final ScheduleAdherenceRequirement sa : this.getTrain()
                .getScheduleAdherenceRequirements()) {
            final Node pointOnRoute = sa.getDestination();
            final int expectedTime = sa.getTimeSinceStartOfWorld();
            for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
                if (entry.getValue() == pointOnRoute) {
                    final long difference = entry.getKey() - expectedTime;
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
    public long getTimeSpentOnUnpreferredTracks(final long time) {
        final BigDecimal convertedTime = convertNewValueToOld(time);
        final SortedMap<Long, Node> nodeEntryTimes = this.getSchedule();
        final SortedMap<Long, Arc> arcEntryTimes = new TreeMap<Long, Arc>();
        for (final SortedMap.Entry<Long, Node> entry : nodeEntryTimes.entrySet()) {
            final Arc a = this.getArcPerStartingNode(entry.getValue());
            if (a == null) {
                continue;
            }
            arcEntryTimes.put(entry.getKey(), a);
        }
        BigDecimal spentTime = BigDecimal.ZERO;
        final Arc leadingArc = this.getLeadingArc(convertedTime);
        /*
         * the time spent in between the nodes is calculated as a difference of their entry times; if we calculated just the
         * time spent traversing the arc, we would have missed wait times and MOWs.
         */
        BigDecimal previousTimeOfEntry = BigDecimal.ZERO;
        Arc previousArc = null;
        for (final SortedMap.Entry<Long, Arc> entry : arcEntryTimes.entrySet()) {
            final BigDecimal currentTimeOfEntry = convertNewValueToOld(entry.getKey());
            final int comparison = currentTimeOfEntry.compareTo(convertedTime);
            if (comparison > 0) {
                // we're not interested in values that are beyong the specified time
                continue;
            }
            final Arc a = entry.getValue();
            if (previousArc != null && !previousArc.isPreferred(this.getTrain())) {
                if (previousArc == leadingArc) {
                    // include the time spent on this track so far
                    spentTime = spentTime.add(convertedTime.subtract(currentTimeOfEntry));
                }
                // include the whole time spent on previous
                spentTime = spentTime.add(currentTimeOfEntry.subtract(previousTimeOfEntry));
            }
            previousTimeOfEntry = currentTimeOfEntry;
            previousArc = a;
        }
        return convertOldValueToNew(spentTime);
    }

    @Override
    public Train getTrain() {
        return this.train;
    }

    @Override
    public WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    private static long convertOldValueToNew(BigDecimal time) {
        return time.multiply(BigDecimal.valueOf(1000)).longValue();
    }

    private static BigDecimal convertNewValueToOld(long time) {
        return BigDecimal.valueOf(time).divide(BigDecimal.valueOf(1000), 10,
                BigDecimal.ROUND_HALF_EVEN);
    }

    @Override
    public Map<Long, Long> getWantTimeDifference() {
        final Map<Long, Long> result = new HashMap<Long, Long>();
        for (final SortedMap.Entry<Long, Node> entry : this.getSchedule().entrySet()) {
            if (entry.getValue() == this.getTrain().getDestination()) {
                final long difference = entry.getKey() - this.getTrain().getWantTime();
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
}
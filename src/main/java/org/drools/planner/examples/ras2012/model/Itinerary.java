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

        private final BigDecimal start, end;

        public Window(final int start, final int end) {
            this.start = BigDecimal.valueOf(start);
            this.end = BigDecimal.valueOf(end);
        }

        public BigDecimal getEnd() {
            return this.end;
        }

        public boolean isInside(final BigDecimal time) {
            if (this.start.compareTo(time) > 0) {
                return false; // window didn't start yet
            }
            if (this.end.compareTo(time) < 0) {
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

    private final Route                            route;

    private final Train                            train;

    private final AtomicBoolean                    scheduleCacheValid = new AtomicBoolean(false);
    private final SortedMap<BigDecimal, Node>      scheduleCache      = new TreeMap<BigDecimal, Node>();
    private final BigDecimal                       trainEntryTime;
    private final List<Arc>                        arcProgression     = new LinkedList<Arc>();
    private final Map<Node, Arc>                   arcPerStartNode    = new HashMap<Node, Arc>();
    private final Map<Node, WaitTime>              nodeWaitTimes      = new HashMap<Node, WaitTime>();
    // FIXME only one window per node; multiple different windows with same node will get lost
    private final Map<Node, Itinerary.Window>      maintenances       = new HashMap<Node, Itinerary.Window>();
    private final Map<BigDecimal, Collection<Arc>> currentlyOccupied  = new HashMap<BigDecimal, Collection<Arc>>();

    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
        this.route = r;
        this.train = t;
        this.trainEntryTime = BigDecimal.valueOf(t.getEntryTime());
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

    private Arc getArcPerStartingNode(final Node n) {
        return this.arcPerStartNode.get(n);
    }

    @Override
    public Collection<Arc> getCurrentlyOccupiedArcs(final BigDecimal timeInMinutes) {
        boolean cleared = false;
        if (!this.scheduleCacheValid.get()) {
            this.currentlyOccupied.clear();
            cleared = true;
        }
        if (cleared || !this.currentlyOccupied.containsKey(timeInMinutes)) {
            final Collection<Arc> a = this.getCurrentlyOccupiedArcsUncached(timeInMinutes);
            this.currentlyOccupied.put(timeInMinutes, a);
            return a;
        }
        return this.currentlyOccupied.get(timeInMinutes);
    }

    private Collection<Arc> getCurrentlyOccupiedArcsUncached(final BigDecimal timeInMinutes) {
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
        final SortedMap<BigDecimal, Node> nodeEntryTimes = this.getSchedule();
        BigDecimal timeArcEntered = null;
        for (final SortedMap.Entry<BigDecimal, Node> entry : nodeEntryTimes.entrySet()) {
            if (entry.getValue() == beginningOfLeadingArc) {
                timeArcEntered = entry.getKey();
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
        if (timeInMinutes.compareTo(this.trainEntryTime) == -1) {
            return null;
        }
        Node previousNode = null;
        for (final SortedMap.Entry<BigDecimal, Node> entry : this.getSchedule().entrySet()) {
            final int comparison = timeInMinutes.compareTo(entry.getKey());
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
    public synchronized SortedMap<BigDecimal, Node> getSchedule() {
        if (!this.scheduleCacheValid.get()) {
            this.scheduleCache.clear();
            int i = 0;
            BigDecimal previousTime = BigDecimal.ZERO;
            Arc previousArc = null;
            for (final Arc currentArc : this.arcProgression) {
                BigDecimal time = BigDecimal.ZERO;
                if (i == 0) {
                    // first item needs to be augmented by the train entry time
                    time = time.add(this.trainEntryTime);
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
                    if (w.isInside(time)) {
                        // the maintenance is ongoing, we have to wait
                        time = w.getEnd();
                    }
                }
                // and store
                this.scheduleCache.put(time, n);
                previousTime = time;
                previousArc = currentArc;
                i++;
            }
            this.scheduleCache.put(
                    previousTime.add(previousArc.getTravellingTimeInMinutes(this.getTrain())),
                    previousArc.getEndingNode(this.getTrain()));
            this.scheduleCacheValid.set(true);
        }
        return Collections.unmodifiableSortedMap(this.scheduleCache);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getScheduleAdherenceStatus() {
        final Map<BigDecimal, BigDecimal> result = new HashMap<BigDecimal, BigDecimal>();
        for (final ScheduleAdherenceRequirement sa : this.getTrain()
                .getScheduleAdherenceRequirements()) {
            final Node pointOnRoute = sa.getDestination();
            final BigDecimal expectedTime = BigDecimal.valueOf(sa.getTimeSinceStartOfWorld());
            for (final SortedMap.Entry<BigDecimal, Node> entry : this.getSchedule().entrySet()) {
                if (entry.getValue() == pointOnRoute) {
                    final BigDecimal difference = entry.getKey().subtract(expectedTime);
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
    public BigDecimal getTimeSpentOnUnpreferredTracks(final BigDecimal time) {
        final SortedMap<BigDecimal, Node> nodeEntryTimes = this.getSchedule();
        final SortedMap<BigDecimal, Arc> arcEntryTimes = new TreeMap<BigDecimal, Arc>();
        for (final SortedMap.Entry<BigDecimal, Node> entry : nodeEntryTimes.entrySet()) {
            final Arc a = this.getArcPerStartingNode(entry.getValue());
            if (a == null) {
                continue;
            }
            arcEntryTimes.put(entry.getKey(), a);
        }
        BigDecimal spentTime = BigDecimal.ZERO;
        final Arc leadingArc = this.getLeadingArc(time);
        /*
         * the time spent in between the nodes is calculated as a difference of their entry times; if we calculated just the
         * time spent traversing the arc, we would have missed wait times and MOWs.
         */
        BigDecimal previousTimeOfEntry = BigDecimal.ZERO;
        Arc previousArc = null;
        for (final SortedMap.Entry<BigDecimal, Arc> entry : arcEntryTimes.entrySet()) {
            final BigDecimal currentTimeOfEntry = entry.getKey();
            final int comparison = currentTimeOfEntry.compareTo(time);
            if (comparison > 0) {
                // we're not interested in values that are beyong the specified time
                continue;
            }
            final Arc a = entry.getValue();
            if (previousArc != null && !previousArc.isPreferred(this.getTrain())) {
                if (previousArc == leadingArc) {
                    // include the time spent on this track so far
                    spentTime = spentTime.add(time.subtract(currentTimeOfEntry));
                }
                // include the whole time spent on previous
                spentTime = spentTime.add(currentTimeOfEntry.subtract(previousTimeOfEntry));
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
    public WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getWantTimeDifference() {
        final Map<BigDecimal, BigDecimal> result = new HashMap<BigDecimal, BigDecimal>();
        for (final SortedMap.Entry<BigDecimal, Node> entry : this.getSchedule().entrySet()) {
            if (entry.getValue() == this.getTrain().getDestination()) {
                final BigDecimal difference = entry.getKey().subtract(
                        BigDecimal.valueOf(this.getTrain().getWantTime()));
                result.put(entry.getKey(), difference);
            }
        }
        return result;
    }

    @Override
    public void removeAllWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            this.scheduleCacheValid.set(false);
        }
        this.nodeWaitTimes.clear();
    }

    @Override
    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            this.scheduleCacheValid.set(false);
            return this.nodeWaitTimes.remove(n);
        } else {
            return null;
        }
    }

    @Override
    public WaitTime setWaitTime(final WaitTime w, final Node n) {
        if (w == null) {
            return this.removeWaitTime(n);
        }
        if (this.getRoute().getWaitPoints().contains(n)) {
            this.scheduleCacheValid.set(false);
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
        for (final SortedMap.Entry<BigDecimal, Node> a : this.getSchedule().entrySet()) {
            sb.append(a.getValue().getId());
            sb.append("@");
            sb.append(a.getKey());
            sb.append(" ");
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public Map<Node, WaitTime> getAllWaitTimes() {
        return Collections.unmodifiableMap(this.nodeWaitTimes);
    }
}
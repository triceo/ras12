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

public final class Itinerary implements ItineraryInterface {

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

    private final Route                             route;

    private final Train                             train;

    private final AtomicBoolean                     nodeEntryTimeCacheValid = new AtomicBoolean(
                                                                                    false);
    private SortedMap<BigDecimal, Node>             nodeEntryTimeCache      = null;
    private final BigDecimal                        trainEntryTime;
    private final List<Arc>                         arcProgression          = new LinkedList<Arc>();
    private final Map<Node, Arc>                    arcPerStartNode         = new HashMap<Node, Arc>();
    private final SortedMap<Node, WaitTime>         nodeWaitTimes           = new TreeMap<Node, WaitTime>();
    // FIXME only one window per node; multiple different windows with same node will get lost
    private final SortedMap<Node, Itinerary.Window> maintenances            = new TreeMap<Node, Itinerary.Window>();

    private int                                     numHaltsFromLastNodeEntryCalculation;

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

    // FIXME dirty, ugly, terrible
    @Override
    public synchronized int countHalts() {
        this.getNodeEntryTimes();
        return this.numHaltsFromLastNodeEntryCalculation;
    }

    private Arc getArcPerStartingNode(final Node n) {
        return this.arcPerStartNode.get(n);
    }

    @Override
    public Arc getCurrentArc(final BigDecimal timeInMinutes) {
        if (timeInMinutes.compareTo(this.trainEntryTime) == -1) {
            return null;
        }
        Node previousNode = null;
        for (final Map.Entry<BigDecimal, Node> e : this.getNodeEntryTimes().entrySet()) {
            final int comparison = timeInMinutes.compareTo(e.getKey());
            if (comparison > 0) {
                previousNode = e.getValue();
                continue;
            } else if (comparison == 0) {
                return this.getArcPerStartingNode(e.getValue());
            } else {
                return this.getArcPerStartingNode(previousNode);
            }
        }
        throw new IllegalStateException("Train probably already finished.");
    }

    @Override
    public Collection<Arc> getCurrentlyOccupiedArcs(final BigDecimal timeInMinutes) {
        Arc leadingArc = null;
        try {
            leadingArc = this.getCurrentArc(timeInMinutes);
        } finally {
            if (leadingArc == null) {
                // train not in the network
                // FIXME train should leave the network gradually, not at once when it reaches destination
                return new HashSet<Arc>();
            }
        }
        final List<Arc> occupiedArcs = new LinkedList<Arc>();
        occupiedArcs.add(leadingArc);
        // calculate how far are we into the leading arc
        final Node beginningOfLeadingArc = leadingArc.getStartingNode(this.getTrain());
        final Map<BigDecimal, Node> nodeEntryTimes = this.getNodeEntryTimes();
        BigDecimal timeArcEntered = null;
        for (final Map.Entry<BigDecimal, Node> entry : nodeEntryTimes.entrySet()) {
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

    @Override
    public BigDecimal getDistanceTravelled(final BigDecimal time) {
        // locate the head of the train
        final Arc leadingArc = this.getCurrentArc(time);
        if (leadingArc == null) {
            if (this.trainEntryTime.compareTo(time) > 0) {
                // train didn't even start
                return BigDecimal.ZERO.setScale(ItineraryInterface.BIGDECIMAL_SCALE,
                        ItineraryInterface.BIGDECIMAL_ROUNDING);
            } else {
                // train finished already
                return this
                        .getRoute()
                        .getLengthInMiles()
                        .setScale(ItineraryInterface.BIGDECIMAL_SCALE,
                                ItineraryInterface.BIGDECIMAL_ROUNDING);
            }
        }
        // calculate whatever we've travelled before we reached the current arc
        BigDecimal travelled = BigDecimal.ZERO;
        BigDecimal lastCheckpointTime = null;
        for (final Map.Entry<BigDecimal, Node> entry : this.getNodeEntryTimes().entrySet()) {
            final Arc a = this.getArcPerStartingNode(entry.getValue());
            if (a == leadingArc) {
                lastCheckpointTime = entry.getKey();
                break;
            } else {
                travelled = travelled.add(a.getLengthInMiles());
            }
        }
        // and now calculate how much we've travelled in the current arc so far
        final BigDecimal timeDifference = time.subtract(lastCheckpointTime);
        final BigDecimal distanceTravelledInArc = Itinerary.getDistanceInMilesFromSpeedAndTime(this
                .getTrain().getMaximumSpeed(leadingArc.getTrackType()), timeDifference);
        return travelled.add(distanceTravelledInArc).setScale(ItineraryInterface.BIGDECIMAL_SCALE,
                ItineraryInterface.BIGDECIMAL_ROUNDING);
    }

    public SortedMap<Node, Itinerary.Window> getMaintenances() {
        return this.maintenances;
    }

    private synchronized SortedMap<BigDecimal, Node> getNodeEntryTimes() {
        if (!this.nodeEntryTimeCacheValid.get()) {
            int halts = 0;
            final SortedMap<BigDecimal, Node> adjusted = new TreeMap<BigDecimal, Node>();
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
                boolean isHalted = false;
                if (wt != null) {
                    isHalted = true;
                    halts++;
                    time = time.add(BigDecimal.valueOf(wt.getMinutesWaitFor()));
                }
                // check for maintenance windows
                if (this.maintenances.containsKey(n)) {
                    // there is a maintenance registered for the next node
                    final Itinerary.Window w = this.maintenances.get(n);
                    if (w.isInside(time)) {
                        if (!isHalted) {
                            halts++;
                        }
                        // the maintenance is ongoing, we have to wait
                        time = w.getEnd();
                    }
                }
                // and store
                adjusted.put(time, n);
                previousTime = time;
                previousArc = currentArc;
                i++;
            }
            adjusted.put(previousTime.add(previousArc.getTravellingTimeInMinutes(this.getTrain())),
                    previousArc.getEndingNode(this.getTrain()));
            this.numHaltsFromLastNodeEntryCalculation = halts;
            this.nodeEntryTimeCache = Collections.unmodifiableSortedMap(adjusted);
            this.nodeEntryTimeCacheValid.set(true);
        }
        return this.nodeEntryTimeCache;
    }

    public Route getRoute() {
        return this.route;
    }

    @Override
    public SortedMap<BigDecimal, Node> getSchedule() {
        final SortedMap<BigDecimal, Node> results = new TreeMap<BigDecimal, Node>();
        for (final Map.Entry<BigDecimal, Node> entry : this.getNodeEntryTimes().entrySet()) {
            results.put(entry.getKey(), entry.getValue());
        }
        return results;
    }

    public Train getTrain() {
        return this.train;
    }

    @Override
    public WaitTime getWaitTime(final Node n) {
        return this.nodeWaitTimes.get(n);
    }

    @Override
    public void removeAllWaitTimes() {
        if (this.nodeWaitTimes.size() > 0) {
            this.nodeEntryTimeCacheValid.set(false);
        }
        this.nodeWaitTimes.clear();
    }

    @Override
    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            this.nodeEntryTimeCacheValid.set(false);
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
            this.nodeEntryTimeCacheValid.set(false);
            final WaitTime previous = this.nodeWaitTimes.get(n);
            this.nodeWaitTimes.put(n, w);
            return previous;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        final int halts = this.numHaltsFromLastNodeEntryCalculation;
        final StringBuilder sb = new StringBuilder();
        sb.append("Itinerary (");
        sb.append(this.getRoute().getLengthInMiles());
        sb.append(" miles, ");
        sb.append(halts);
        sb.append(" halts): ");
        for (final Map.Entry<BigDecimal, Node> a : this.getNodeEntryTimes().entrySet()) {
            sb.append(a.getValue().getId());
            sb.append("@");
            sb.append(a.getKey());
            sb.append(" ");
        }
        sb.append(".");
        return sb.toString();
    }
}
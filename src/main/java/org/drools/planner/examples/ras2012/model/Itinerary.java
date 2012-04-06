package org.drools.planner.examples.ras2012.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Itinerary implements ItineraryInterface {

    private static final class Window {

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

    private static final Logger logger = LoggerFactory.getLogger(Itinerary.class);

    private static BigDecimal getDistanceInMilesFromSpeedAndTime(final int speedInMPH,
            final BigDecimal time) {
        final BigDecimal milesPerHour = BigDecimal.valueOf(speedInMPH);
        final BigDecimal milesPerMinute = milesPerHour.divide(BigDecimal.valueOf(60), 5,
                BigDecimal.ROUND_HALF_DOWN);
        return milesPerMinute.multiply(time);
    }

    private static BigDecimal getTimeInMinutesFromSpeedAndDistance(final int speedInMPH,
            final BigDecimal distanceInMiles) {
        final BigDecimal milesPerHour = BigDecimal.valueOf(speedInMPH);
        final BigDecimal hours = distanceInMiles
                .divide(milesPerHour, 5, BigDecimal.ROUND_HALF_DOWN);
        return hours.multiply(BigDecimal.valueOf(60));
    }

    private static boolean isLarger(final BigDecimal left, final BigDecimal right) {
        return left.compareTo(right) > 0;
    }

    private final Route                             route;
    private final Train                             train;
    private final BigDecimal                        trainEntryTime;
    private final List<Arc>                         arcProgression = new LinkedList<Arc>();
    private final SortedMap<Node, WaitTime>         nodeWaitTimes  = new TreeMap<Node, WaitTime>();

    // FIXME only one window per node; multiple different windows with same node will get lost
    private final SortedMap<Node, Itinerary.Window> maintenances   = new TreeMap<Node, Itinerary.Window>();

    private int                                     numHaltsFromLastNodeEntryCalculation;

    public Itinerary(final Route r, final Train t,
            final Collection<MaintenanceWindow> maintenanceWindows) {
        this.route = r;
        this.train = t;
        this.trainEntryTime = BigDecimal.valueOf(t.getEntryTime());
        // assemble the node-traversal information
        Arc currentArc = null;
        while ((currentArc = this.route.getNextArc(currentArc)) != null) {
            this.pass(currentArc);
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
        this.getArcEntryTimes();
        return this.numHaltsFromLastNodeEntryCalculation;
    }

    private Map<Arc, BigDecimal> getArcEntryTimes() {
        int halts = 0;
        final Map<Arc, BigDecimal> adjusted = new HashMap<Arc, BigDecimal>();
        int i = 0;
        Arc previousArc = null;
        for (final Arc currentArc : this.arcProgression) {
            BigDecimal time = currentArc.getTravellingTimeInMinutes(this.getTrain());
            if (i == 0) {
                // first item needs to be augmented by the train entry time
                time = time.add(this.trainEntryTime);
            } else {
                // otherwise we need to convert a relative time to an absolute time by adding the previous node's time
                time = time.add(adjusted.get(previousArc));
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
            adjusted.put(currentArc, time);
            previousArc = currentArc;
            i++;
        }
        this.numHaltsFromLastNodeEntryCalculation = halts;
        return Collections.unmodifiableMap(adjusted);
    }

    @Override
    public Arc getCurrentArc(final BigDecimal timeInMinutes) {
        for (final Map.Entry<Arc, BigDecimal> e : this.getArcEntryTimes().entrySet()) {
            final Arc currentArc = e.getKey();
            final BigDecimal nodeEntryTime = e.getValue();
            if (Itinerary.isLarger(timeInMinutes, nodeEntryTime)) {
                continue;
            } else {
                return currentArc;
            }
        }
        throw new IllegalStateException("Train is no longer en route at the time: " + timeInMinutes);
    }

    @Override
    public Collection<Arc> getCurrentlyOccupiedArcs(final BigDecimal timeInMinutes) {
        // get the distance that the end of the train has made since the start of time
        final BigDecimal distanceTravelled = this.getDistanceTravelled(timeInMinutes);
        final BigDecimal endOfTrainIsAt = distanceTravelled.subtract(this.getTrain().getLength());
        // and now locate the arc that the end of the train is at
        BigDecimal arcEndsAtMile = BigDecimal.ZERO;
        Arc terminalArc = null;
        Arc previousArc = null;
        for (int i = 0; i < this.arcProgression.size(); i++) {
            final Arc a = this.arcProgression.get(i);
            arcEndsAtMile = arcEndsAtMile.add(a.getLengthInMiles());
            final int comparison = endOfTrainIsAt.compareTo(arcEndsAtMile);
            if (comparison == 0) {
                // the current arc is where the node ends
                terminalArc = a;
                break;
            } else if (comparison == -1) {
                // the train ends on some of the following arcs
                previousArc = a;
            } else {
                // train already ended
                terminalArc = previousArc;
                break;
            }
        }
        // locate the head of the train
        Arc leadingArc;
        try {
            leadingArc = this.getCurrentArc(timeInMinutes);
        } catch (final IllegalStateException ex) {
            // train is no longer in the network
            // FIXME train leaves the network when the head enters the depot; it should be the tail
            return new LinkedList<Arc>();
        }
        // and now enumerate every arc from the terminal to the leading
        Arc currentArc = terminalArc;
        final List<Arc> result = new LinkedList<Arc>();
        result.add(currentArc);
        while ((currentArc = this.route.getNextArc(currentArc)) != leadingArc) {
            result.add(currentArc);
        }
        result.add(leadingArc);
        return result;
    }

    @Override
    public BigDecimal getDistanceTravelled(final BigDecimal time) {
        // locate the head of the train
        Arc leadingArc;
        try {
            leadingArc = this.getCurrentArc(time);
            if (leadingArc == null) {
                return BigDecimal.ZERO;
            }
        } catch (final IllegalStateException ex) {
            // train is already in the destination
            return this.getRoute().getLengthInMiles();
        }
        // calculate whatever we've travelled before we reached the current arc
        BigDecimal travelled = BigDecimal.ZERO;
        BigDecimal lastCheckpointTime = null;
        for (final Map.Entry<Arc, BigDecimal> entry : this.getArcEntryTimes().entrySet()) {
            final Arc a = entry.getKey();
            if (a == leadingArc) {
                lastCheckpointTime = entry.getValue();
                break;
            } else {
                travelled = travelled.add(a.getLengthInMiles());
            }
        }
        // and now calculate how much we've travelled in the current arc so far
        final BigDecimal timeDifference = time.subtract(lastCheckpointTime);
        final BigDecimal distanceTravelledInArc = Itinerary.getDistanceInMilesFromSpeedAndTime(this
                .getTrain().getMaximumSpeed(leadingArc.getTrackType()), timeDifference);
        return travelled.add(distanceTravelledInArc);
    }

    @Override
    public Node getNextNodeToReach(final BigDecimal timeInMinutes) {
        return this.getCurrentArc(timeInMinutes).getEndingNode(this.getTrain());
    }

    public Route getRoute() {
        return this.route;
    }

    @Override
    public SortedMap<Node, BigDecimal> getSchedule() {
        final SortedMap<Node, BigDecimal> results = new TreeMap<Node, BigDecimal>();
        for (final Map.Entry<Arc, BigDecimal> entry : this.getArcEntryTimes().entrySet()) {
            results.put(entry.getKey().getStartingNode(this.getTrain()), entry.getValue());
        }
        return results;
    }

    public Train getTrain() {
        return this.train;
    }

    // FIXME prevent calling from outside constructor
    private void pass(final Arc a) {
        final BigDecimal distance = a.getLengthInMiles();
        final BigDecimal minutesPerArc = Itinerary.getTimeInMinutesFromSpeedAndDistance(
                this.train.getMaximumSpeed(a.getTrackType()), distance);
        // and now mark passing another node
        this.arcProgression.add(a);
        // calculate average speed at this arc
        final BigDecimal result = distance.divide(
                minutesPerArc.divide(BigDecimal.valueOf(60), 5, BigDecimal.ROUND_UP), 5,
                BigDecimal.ROUND_UP);
        final long speed = Math.round(result.doubleValue());
        final Node n = a.getEndingNode(this.getRoute());
        Itinerary.logger.debug(n + " (" + distance + " miles) reached in " + minutesPerArc
                + " min.; total " + minutesPerArc + " min., avg. speed " + speed + " mph.");
    }

    @Override
    public WaitTime removeWaitTime(final Node n) {
        if (this.nodeWaitTimes.containsKey(n)) {
            return this.nodeWaitTimes.remove(n);
        } else {
            return null;
        }
    }

    @Override
    public WaitTime setWaitTime(final WaitTime w, final Node n) {
        if (this.getRoute().getWaitPoints().contains(n)) {
            final WaitTime previous = this.nodeWaitTimes.get(n);
            this.nodeWaitTimes.put(n, w);
            return previous;
        } else {
            return null;
        }
    }

    public boolean toCSV(final OutputStream os) {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os))) {
            w.write("node;time");
            w.newLine();
            for (final Map.Entry<Arc, BigDecimal> times : this.getArcEntryTimes().entrySet()) {
                w.write(String.valueOf(times.getKey().getStartingNode(this.getTrain()).getId()));
                w.write(";");
                w.write(times.getValue().toString());
                w.newLine();
            }
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    @Override
    public String toString() {
        final Map<Arc, BigDecimal> adjustedEntryTimes = this.getArcEntryTimes();
        final int halts = this.numHaltsFromLastNodeEntryCalculation;
        final StringBuilder sb = new StringBuilder();
        sb.append("Itinerary (~");
        sb.append(this.getRoute().getLengthInMiles().intValue());
        sb.append(" miles, ~");
        sb.append(halts);
        sb.append(" halts): ");
        for (final Map.Entry<Arc, BigDecimal> a : adjustedEntryTimes.entrySet()) {
            sb.append(a.getKey().getStartingNode(this.getTrain()).getId());
            sb.append("@");
            sb.append(a.getValue());
            sb.append(" ");
        }
        sb.append(".");
        return sb.toString();
    }
}
package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutePlan {

    public static final class Itinerary {

        private static final class Window {

            public boolean isInside(BigDecimal time) {
                if (start.compareTo(time) > 0)
                    return false; // window didn't start yet
                if (end.compareTo(time) < 0)
                    return false; // window is already over
                return true;
            }

            private final BigDecimal start, end;

            public BigDecimal getEnd() {
                return end;
            }

            public Window(int start, int end) {
                this.start = BigDecimal.valueOf(start);
                this.end = BigDecimal.valueOf(end);
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

        private static boolean isLarger(final BigDecimal left, final BigDecimal right) {
            return left.compareTo(right) > 0;
        }

        private final RoutePlan                plan;
        private final BigDecimal               trainEntryTime;
        private final AtomicInteger            idGenerator        = new AtomicInteger(1);
        private final Map<Integer, Arc>        arcProgression     = new TreeMap<Integer, Arc>();
        private final Map<Integer, Node>       nodeProgression    = new TreeMap<Integer, Node>();
        private final Map<Integer, BigDecimal> nodeDistances      = new TreeMap<Integer, BigDecimal>();
        private final Map<Integer, BigDecimal> arcTravellingTimes = new TreeMap<Integer, BigDecimal>();
        private final Map<Node, WaitTime>      nodeWaitTimes      = new HashMap<Node, WaitTime>();
        // FIXME only one window per node; multiple different windows with same node will get lost
        private final Map<Node, Window>        maintenances       = new HashMap<Node, Window>();

        public Itinerary(final RoutePlan plan, Collection<MaintenanceWindow> maintenanceWindows) {
            this.plan = plan;
            this.trainEntryTime = BigDecimal.valueOf(this.plan.getTrain().getEntryTime());
            // initialize data structures with the first node
            this.nodeProgression.put(0, plan.getTrain().getOrigin());
            this.nodeDistances.put(0, this.plan.getRoute().getFirstArc().getLengthInMiles());
            this.arcTravellingTimes.put(0, BigDecimal.ZERO);
            for (MaintenanceWindow mow : maintenanceWindows) {
                Node n = this.plan.getTrain().isEastbound() ? mow.getWestNode() : mow.getEastNode();
                Window w = new Window(mow.getStartingMinute(), mow.getEndingMinute());
                this.maintenances.put(n, w);
            }
        }

        private Map<Integer, BigDecimal> getNodeEntryTimes() {
            final Map<Integer, BigDecimal> adjusted = new TreeMap<Integer, BigDecimal>();
            final SortedSet<Integer> keys = new TreeSet<Integer>(this.nodeDistances.keySet());
            int i = 0;
            for (final int key : keys) {
                BigDecimal time = this.arcTravellingTimes.get(key);
                if (i == 0) {
                    // first item needs to be augmented by the train entry time
                    time = time.add(this.trainEntryTime);
                } else {
                    // otherwise we need to convert a relative time to an absolute time by adding the previous node's time
                    time = time.add(adjusted.get(key - 1));
                }
                // now adjust for node wait time, should there be any
                final Node n = this.nodeProgression.get(key);
                final WaitTime wt = this.nodeWaitTimes.get(n);
                if (wt != null) {
                    time = time.add(BigDecimal.valueOf(wt.getMinutesWaitFor()));
                }
                // check for maintenance windows
                if (this.maintenances.containsKey(n)) {
                    // there is a maintenance registered for the next node
                    Window w = this.maintenances.get(n);
                    if (w.isInside(time)) {
                        // the maintenance is ongoing, we have to wait
                        time = w.getEnd();
                    }
                }
                // and store
                adjusted.put(key, time);
                i++;
            }
            return Collections.unmodifiableMap(adjusted);
        }

        private int getArcId(final Arc arc) {
            for (final Map.Entry<Integer, Arc> e : this.arcProgression.entrySet()) {
                if (e.getValue() == arc) {
                    return e.getKey();
                }
            }
            throw new IllegalStateException("No such arc in the itinerary: " + arc);
        }

        public Arc getCurrentArc(final BigDecimal timeInMinutes) {
            for (final Map.Entry<Integer, BigDecimal> e : this.getNodeEntryTimes().entrySet()) {
                final int nodeId = e.getKey();
                final BigDecimal nodeEntryTime = e.getValue();
                if (Itinerary.isLarger(timeInMinutes, nodeEntryTime)) {
                    continue;
                } else {
                    return this.arcProgression.get(nodeId);
                }
            }
            throw new IllegalStateException("Train is no longer en route at the time: "
                    + timeInMinutes);
        }

        public Collection<Arc> getCurrentlyOccupiedArcs(final BigDecimal timeInMinutes) {
            // locate the head of the train
            Arc leadingArc;
            try {
                leadingArc = this.getCurrentArc(timeInMinutes);
            } catch (final IllegalStateException ex) {
                // train is no longer in the network
                // FIXME train leaves the network when the head enters the depot; it should be the tail
                return new LinkedList<Arc>();
            }
            final int leadingArcId = this.getArcId(leadingArc);
            BigDecimal unaccountedTrainLength = this.plan.getTrain().getLength();
            // now figure out how far the head is into the arc
            final int previousArcId = leadingArcId - 1;
            final Map<Integer, BigDecimal> adjustedNodeEntryTimes = this.getNodeEntryTimes();
            final BigDecimal lastCheckpointTime = adjustedNodeEntryTimes.containsKey(previousArcId) ? adjustedNodeEntryTimes
                    .get(leadingArcId - 1) : this.trainEntryTime;
            final BigDecimal timeDifference = timeInMinutes.subtract(lastCheckpointTime);
            final BigDecimal distanceTravelledInArc = Itinerary
                    .getDistanceInMilesFromSpeedAndTime(
                            this.plan.getTrain().getMaximumSpeed(leadingArc.getTrackType()),
                            timeDifference);
            unaccountedTrainLength = unaccountedTrainLength.subtract(distanceTravelledInArc);
            final Collection<Arc> occupiedArcs = new LinkedList<Arc>();
            occupiedArcs.add(leadingArc);
            // and now find any other arcs that our train may be blocking towards the read
            for (int arcId = leadingArcId - 1; arcId >= 0; arcId--) {
                if (unaccountedTrainLength.compareTo(BigDecimal.ZERO) < 0) {
                    // we've found the arc where the train ends
                    break;
                } else {
                    final Arc arc = this.arcProgression.get(arcId);
                    final BigDecimal arcLength = arc.getLengthInMiles();
                    unaccountedTrainLength = unaccountedTrainLength.subtract(arcLength);
                    occupiedArcs.add(arc);
                }
            }
            return occupiedArcs;
        }

        public Node getNextStop(final BigDecimal timeInMinutes) {
            return this.getTerminatingNode(this.getCurrentArc(timeInMinutes));
        }

        private Node getTerminatingNode(final Arc a) {
            if (this.plan.getTrain().isEastbound()) {
                return a.getEndingNode();
            } else {
                return a.getStartingNode();
            }
        }

        private void pass(final Arc a, final BigDecimal distance, final BigDecimal minutesPerArc) {
            // get previous node enter time, so that we can calculate the time difference
            final Node n = this.getTerminatingNode(a);
            // and now mark passing another node
            final int id = this.idGenerator.getAndIncrement();
            this.arcProgression.put(id, a);
            this.nodeProgression.put(id, n);
            this.nodeDistances.put(id, distance);
            this.arcTravellingTimes.put(id, minutesPerArc);
            // calculate average speed at this arc
            final BigDecimal result = distance.divide(
                    minutesPerArc.divide(BigDecimal.valueOf(60), 5, BigDecimal.ROUND_UP), 5,
                    BigDecimal.ROUND_UP);
            final long speed = Math.round(result.doubleValue());
            Itinerary.logger.debug(n + " (" + distance + " miles) reached in " + minutesPerArc
                    + " min.; total " + minutesPerArc + " min., avg. speed " + speed + " mph.");
        }

        public WaitTime removeWaitTime(final Node n) {
            if (this.nodeWaitTimes.containsKey(n)) {
                return this.nodeWaitTimes.remove(n);
            } else {
                return null;
            }
        }

        public boolean setWaitTime(final WaitTime w, final Node n) {
            if (this.plan.getRoute().getWaitPoints().contains(n)) {
                this.nodeWaitTimes.put(n, w);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            Map<Integer, BigDecimal> adjustedEntryTimes = this.getNodeEntryTimes();
            final StringBuilder sb = new StringBuilder();
            sb.append("Itinerary (");
            sb.append(this.plan.getRoute().getLengthInMiles());
            sb.append(" miles): ");
            for (int i = 0; i < this.idGenerator.get(); i++) {
                sb.append(this.nodeProgression.get(i));
                sb.append("@");
                sb.append(adjustedEntryTimes.get(i));
                sb.append(" ");
            }
            sb.append(".");
            return sb.toString();
        }
    }

    private static BigDecimal getTimeInMinutesFromSpeedAndDistance(final int speedInMPH,
            final BigDecimal distanceInMiles) {
        final BigDecimal milesPerHour = BigDecimal.valueOf(speedInMPH);
        final BigDecimal hours = distanceInMiles
                .divide(milesPerHour, 5, BigDecimal.ROUND_HALF_DOWN);
        return hours.multiply(BigDecimal.valueOf(60));
    }

    private final Integer   horizonInMinutes = RAS2012Solution.PLANNING_HORIZON_MINUTES;

    private final Train     train;

    private final Itinerary itinerary;

    private final Route     route;

    public RoutePlan(final Route r, final Train t, Collection<MaintenanceWindow> maintenances) {
        if (!r.isPossibleForTrain(t)) {
            throw new IllegalArgumentException("Route " + r + " not possible for train " + t);
        }
        this.route = r;
        this.train = t;
        this.itinerary = new Itinerary(this, maintenances);
        this.assembleItinerary();
    }

    private void assembleItinerary() {
        Arc currentArc = null;
        while ((currentArc = this.route.getNextArc(currentArc)) != null) {
            final int currentSpeed = this.train.getMaximumSpeed(currentArc.getTrackType());
            final BigDecimal arcLength = currentArc.getLengthInMiles();
            final BigDecimal timeItTakes = RoutePlan.getTimeInMinutesFromSpeedAndDistance(
                    currentSpeed, arcLength);
            this.itinerary.pass(currentArc, arcLength, timeItTakes);
        }
    }

    public Integer getHorizonInMinutes() {
        return this.horizonInMinutes;
    }

    public Itinerary getItinerary() {
        return this.itinerary;
    }

    public Route getRoute() {
        return this.route;
    }

    public Train getTrain() {
        return this.train;
    }

    @Override
    public String toString() {
        return "RoutePlan [train=" + this.train + ", route=" + this.route + "]";
    }
}
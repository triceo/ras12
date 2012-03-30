package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutePlan {

    public static final class Itinerary {

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
        private final AtomicInteger            idGenerator     = new AtomicInteger(0);
        private final Map<Integer, Arc>        arcProgression  = new TreeMap<Integer, Arc>();
        private final Map<Integer, Node>       nodeProgression = new TreeMap<Integer, Node>();
        private final Map<Integer, BigDecimal> nodeDistances   = new TreeMap<Integer, BigDecimal>();

        private final Map<Integer, BigDecimal> nodeEntryTimes  = new TreeMap<Integer, BigDecimal>();

        public Itinerary(final RoutePlan plan) {
            this.plan = plan;
            this.trainEntryTime = BigDecimal.valueOf(this.plan.getTrain().getEntryTime());
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
            for (final Map.Entry<Integer, BigDecimal> e : this.nodeEntryTimes.entrySet()) {
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
            final BigDecimal lastCheckpointTime = this.nodeEntryTimes.containsKey(previousArcId) ? this.nodeEntryTimes
                    .get(leadingArcId - 1) : BigDecimal.ZERO;
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

        private void pass(final Arc a, final BigDecimal distance,
                final BigDecimal relativeTimeOfArrival) {
            // get previous node enter time, so that we can calculate the time difference
            final BigDecimal actualTimeOfArrival = relativeTimeOfArrival.add(this.trainEntryTime);
            BigDecimal relativeTime = BigDecimal.ZERO;
            if (this.nodeEntryTimes.isEmpty()) {
                relativeTime = actualTimeOfArrival;
            } else {
                final int previousId = this.idGenerator.get() - 1;
                final BigDecimal previousTime = this.nodeEntryTimes.get(previousId);
                relativeTime = actualTimeOfArrival.subtract(previousTime);
            }
            final Node n = this.getTerminatingNode(a);
            if (!Itinerary.isLarger(relativeTime, BigDecimal.ZERO)) {
                throw new IllegalStateException("Relative time at " + n
                        + " is negative; suggests a bug in itinerary calculation code.");
            }
            // and now mark passing another node
            final int id = this.idGenerator.getAndIncrement();
            this.arcProgression.put(id, a);
            this.nodeProgression.put(id, n);
            this.nodeDistances.put(id, distance);
            this.nodeEntryTimes.put(id, actualTimeOfArrival);
            // calculate average speed at this arc
            final BigDecimal result = distance.divide(
                    relativeTime.divide(BigDecimal.valueOf(60), 5, BigDecimal.ROUND_UP), 5,
                    BigDecimal.ROUND_UP);
            final long speed = Math.round(result.doubleValue());
            Itinerary.logger.debug(n + " (" + distance + " miles) reached in " + relativeTime
                    + " min.; total " + actualTimeOfArrival + " min., avg. speed " + speed
                    + " mph.");
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Itinerary:");
            for (int i = 0; i < this.idGenerator.get(); i++) {
                sb.append(this.nodeProgression.get(i));
                sb.append("@");
                sb.append(this.nodeEntryTimes.get(i));
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

    public RoutePlan(final Route r, final Train t) {
        if (!r.isPossibleForTrain(t)) {
            throw new IllegalArgumentException("Route " + r + " not possible for train " + t);
        }
        this.route = r;
        this.train = t;
        this.itinerary = new Itinerary(this);
        this.assembleItinerary();
    }

    private void assembleItinerary() {
        Arc currentArc = null;
        BigDecimal totalTime = BigDecimal.ZERO;
        while ((currentArc = this.route.getNextArc(currentArc)) != null) {
            final int currentSpeed = this.train.getMaximumSpeed(currentArc.getTrackType());
            final BigDecimal arcLength = currentArc.getLengthInMiles();
            final BigDecimal timeItTakes = RoutePlan.getTimeInMinutesFromSpeedAndDistance(
                    currentSpeed, arcLength);
            totalTime = totalTime.add(timeItTakes);
            this.itinerary.pass(currentArc, arcLength, totalTime);
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
package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.SortedMap;

import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;

public class Converter {

    public static BigDecimal calculateActualDistanceTravelled(final Itinerary i, final long time) {
        final SortedMap<Long, Node> schedule = i.getSchedule();
        final SortedMap<Long, Node> head = schedule.headMap(time);
        if (head.values().contains(i.getTrain().getDestination())) {
            // train is already finished at the time
            return Converter.calculateActualDistanceTravelled(i, i.getTrain().getDestination());
        } else {
            // train not yet finished
            final Arc leadingArc = i.getRoute().getProgression()
                    .getWithOriginNode(schedule.get(head.lastKey()));
            BigDecimal result = Converter.calculateActualDistanceTravelled(i,
                    leadingArc.getOrigin(i.getRoute()));
            result = result.add(Converter.getDistanceTravelledInTheArc(i, leadingArc, time));
            return result;
        }
    }

    private static BigDecimal calculateActualDistanceTravelled(final Itinerary i,
            final Node position) {
        return i.getRoute().getProgression().getDistance(i.getTrain().getOrigin(), position);
    }

    public static BigDecimal getDistanceInMilesFromSpeedAndTime(final int speedInMPH,
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

    public static BigDecimal getDistanceTravelledInTheArc(final Itinerary i, final Arc a,
            final long time) {
        final long timeTravelledInLeadingArc = time - Converter.getNearestPastCheckpoint(i, time);
        BigDecimal distanceTravelledInLeadingArc = BigDecimal.ZERO;
        if (timeTravelledInLeadingArc > 0) {
            // if we're inside the leading arc (and not exactly in the origin node), count it
            distanceTravelledInLeadingArc = Converter.getDistanceInMilesFromSpeedAndTime(i
                    .getTrain().getMaximumSpeed(a.getTrack()), timeTravelledInLeadingArc);
        }
        return distanceTravelledInLeadingArc;
    }

    private static long getNearestPastCheckpoint(final Itinerary i, final long time) {
        return i.getSchedule().headMap(time + 1).lastKey();
    }

    public static long getTimeFromSpeedAndDistance(final int speedInMPH, final BigDecimal distance) {
        return distance.divide(BigDecimal.valueOf(speedInMPH), 10, BigDecimal.ROUND_HALF_EVEN)
                .multiply(BigDecimal.valueOf(3600000)).longValue();
    }

}

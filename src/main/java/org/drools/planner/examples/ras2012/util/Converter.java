package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.SortedMap;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.Node;

public class Converter {

    public static final int BIGDECIMAL_ROUNDING = BigDecimal.ROUND_HALF_EVEN;
    public static final int BIGDECIMAL_SCALE    = 7;

    private static BigDecimal calculateActualDistanceTravelled(final Itinerary i,
            final Node position) {
        return i.getRoute().getProgression().getDistance(i.getTrain().getOrigin(), position);
    }

    public static BigDecimal getDistanceFromSpeedAndTime(final BigDecimal speedInMPH,
            final long timeInMilliseconds) {
        final BigDecimal timeInSeconds = BigDecimal.valueOf(timeInMilliseconds)
                .divide(BigDecimal.valueOf(1000), Converter.BIGDECIMAL_SCALE,
                        Converter.BIGDECIMAL_ROUNDING);
        final BigDecimal timeInMinutes = timeInSeconds.divide(BigDecimal.valueOf(60),
                Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
        final BigDecimal timeInHours = timeInMinutes.divide(BigDecimal.valueOf(60),
                Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
        return speedInMPH.multiply(timeInHours);
    }

    public static BigDecimal getDistanceTravelled(final Itinerary i, final Arc a, final long time) {
        final SortedMap<Long, Node> schedule = i.getSchedule().headMap(time);
        final long nearestPastCheckpoint = schedule.headMap(time).lastKey();
        if (schedule.get(nearestPastCheckpoint) != a.getOrigin(i.getRoute())) {
            throw new IllegalArgumentException("Arc is not a leading arc at the given time.");
        }
        final long timeTravelledInLeadingArc = time - nearestPastCheckpoint;
        return Converter.getDistanceFromSpeedAndTime(i.getTrain().getMaximumSpeed(a.getTrack()),
                timeTravelledInLeadingArc).min(a.getLength());
    }

    public static BigDecimal getDistanceTravelled(final Itinerary i, final long time) {
        final SortedMap<Long, Node> schedule = i.getSchedule();
        final SortedMap<Long, Node> head = schedule.headMap(time);
        if (head.size() == 0) {
            return BigDecimal.ZERO;
        } else if (head.values().contains(i.getTrain().getDestination())) {
            // train is already finished at the time
            return Converter.calculateActualDistanceTravelled(i, i.getTrain().getDestination());
        } else {
            // train not yet finished
            final Arc leadingArc = i.getRoute().getProgression()
                    .getWithOriginNode(schedule.get(head.lastKey()));
            BigDecimal result = Converter.calculateActualDistanceTravelled(i,
                    leadingArc.getOrigin(i.getRoute()));
            result = result.add(Converter.getDistanceTravelled(i, leadingArc, time));
            return result;
        }
    }

    public static long getTimeFromSpeedAndDistance(final BigDecimal speedInMPH,
            final BigDecimal distance) {
        return distance
                .divide(speedInMPH, Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING)
                .multiply(BigDecimal.valueOf(3600000)).setScale(0, Converter.BIGDECIMAL_ROUNDING)
                .longValue();
    }

    public static long getTimeFromSpeedAndDistance(final int speedInMPH, final BigDecimal distance) {
        return Converter.getTimeFromSpeedAndDistance(BigDecimal.valueOf(speedInMPH), distance);
    }

}

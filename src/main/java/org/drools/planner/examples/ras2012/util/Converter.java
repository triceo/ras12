package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

public class Converter {

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

    public static long getTimeFromSpeedAndDistance(final int speedInMPH, final BigDecimal distance) {
        return distance.divide(BigDecimal.valueOf(speedInMPH), 10, BigDecimal.ROUND_HALF_EVEN)
                .multiply(BigDecimal.valueOf(3600000)).longValue();
    }

}

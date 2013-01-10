package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

public class Converter {

    public static final int        BIGDECIMAL_ROUNDING = BigDecimal.ROUND_HALF_EVEN;
    public static final int        BIGDECIMAL_SCALE    = 7;
    public static final BigDecimal SIXTY               = BigDecimal.valueOf(60);
    public static final BigDecimal THOUSAND            = BigDecimal.valueOf(1000);

    public static BigDecimal getDistanceFromSpeedAndTime(final BigDecimal speedInMPH,
            final long timeInMilliseconds) {
        final BigDecimal timeInSeconds = BigDecimal.valueOf(timeInMilliseconds).divide(
                Converter.THOUSAND, Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
        final BigDecimal timeInMinutes = timeInSeconds.divide(Converter.SIXTY,
                Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
        final BigDecimal timeInHours = timeInMinutes.divide(Converter.SIXTY,
                Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
        return speedInMPH.multiply(timeInHours);
    }

    public static long getTimeFromSpeedAndDistance(final BigDecimal speedInMPH,
            final BigDecimal distanceInMiles) {
        final double speed = speedInMPH.doubleValue();
        final double distance = distanceInMiles.doubleValue();
        final double result = (distance * 3600000.0d) / speed;
        return Math.round(result);
    }
}

package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

public class Converter {

    public static final int BIGDECIMAL_ROUNDING = BigDecimal.ROUND_HALF_EVEN;
    public static final int BIGDECIMAL_SCALE    = 7;

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

package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

public class Converter {

    public static long convertOldValueToNew(BigDecimal time) {
        return time.multiply(BigDecimal.valueOf(1000)).longValue();
    }

    public static BigDecimal convertNewValueToOld(long time) {
        return BigDecimal.valueOf(time).divide(BigDecimal.valueOf(1000), 10,
                BigDecimal.ROUND_HALF_EVEN);
    }

}

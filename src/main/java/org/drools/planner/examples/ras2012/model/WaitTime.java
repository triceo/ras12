package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WaitTime {

    private static final Map<Integer, WaitTime> waitTimes         = new HashMap<Integer, WaitTime>();

    private static final TimeUnit               DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Get WaitTime instance with specified wait time.
     * 
     * @param i How many minutes of waiting should this WaitTime inflict.
     * @return
     */
    public static synchronized WaitTime getWaitTime(final int i) {
        if (i < 1) {
            throw new IllegalArgumentException("Wait time must be bigger than zero.");
        }
        if (!WaitTime.waitTimes.containsKey(i)) {
            final WaitTime w = new WaitTime(i);
            WaitTime.waitTimes.put(i, w);
            return w;
        }
        return WaitTime.waitTimes.get(i);
    }

    private final long timeToWaitFor;

    private WaitTime(final long minutes) {
        this.timeToWaitFor = WaitTime.DEFAULT_TIME_UNIT.convert(minutes, TimeUnit.MINUTES);
    }

    public long getWaitFor(final TimeUnit unit) {
        return unit.convert(this.timeToWaitFor, WaitTime.DEFAULT_TIME_UNIT);
    }

    @Override
    public String toString() {
        return "WaitTime [" + this.getWaitFor(TimeUnit.MINUTES) + " min.]";
    }

}

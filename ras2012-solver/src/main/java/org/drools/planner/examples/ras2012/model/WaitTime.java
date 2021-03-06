package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.planner.examples.ras2012.util.model.ArcProgression;

/**
 * At specific parts of the route, called "wait points", the train can be stopped. (See {@link ArcProgression#getWaitPoints()}
 * and {@link Itinerary#setWaitTime(Node, WaitTime)}.) Instances of this class represent the length of these stops.
 */
public class WaitTime {

    private static final Map<Long, WaitTime> waitTimes         = new HashMap<>();

    private static final TimeUnit            DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Get WaitTime instance with specified wait time.
     * 
     * @param i How many minutes of waiting should this WaitTime inflict.
     */
    public static WaitTime getWaitTime(final int i) {
        return WaitTime.getWaitTime(i, TimeUnit.MINUTES);
    }

    /**
     * Get WaitTime instance with specified wait time.
     * 
     * @param i How many units of time of waiting should this WaitTime inflict.
     * @param unit In which units would that be.
     */
    public static WaitTime getWaitTime(final long i, final TimeUnit unit) {
        if (i < 1) {
            throw new IllegalArgumentException("Wait time must be bigger than zero: " + i);
        }
        final long actualTime = WaitTime.DEFAULT_TIME_UNIT.convert(i, unit);
        if (!WaitTime.waitTimes.containsKey(actualTime)) {
            final WaitTime w = new WaitTime(actualTime);
            WaitTime.waitTimes.put(actualTime, w);
            return w;
        }
        return WaitTime.waitTimes.get(actualTime);
    }

    private final long timeToWaitFor;

    private WaitTime(final long milliseconds) {
        this.timeToWaitFor = WaitTime.DEFAULT_TIME_UNIT
                .convert(milliseconds, TimeUnit.MILLISECONDS);
    }

    public long getWaitFor(final TimeUnit unit) {
        return unit.convert(this.timeToWaitFor, WaitTime.DEFAULT_TIME_UNIT);
    }

    @Override
    public String toString() {
        return "WaitTime [" + this.getWaitFor(TimeUnit.MINUTES) + " min.]";
    }

}

package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.TimeUnit;

public class MaintenanceWindow extends Section {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final long            start;
    private final long            end;

    public MaintenanceWindow(final Node westNode, final Node eastNode, final long time1,
            final long time2) {
        super(westNode, eastNode);
        if (time1 < 0 || time2 < 0) {
            throw new IllegalArgumentException("Neither time can be less than zero.");
        }
        this.start = MaintenanceWindow.DEFAULT_TIME_UNIT.convert(Math.min(time1, time2),
                TimeUnit.MINUTES);
        this.end = MaintenanceWindow.DEFAULT_TIME_UNIT.convert(Math.max(time1, time2),
                TimeUnit.MINUTES);
    }

    /**
     * Get time when this maintenance window ends. (Inclusive.)
     * 
     * @param unit Unit in which to return the time.
     * @return Time since the beginning of the world.
     */
    public long getEnd(final TimeUnit unit) {
        return unit.convert(this.end, MaintenanceWindow.DEFAULT_TIME_UNIT);
    }

    /**
     * Get time when this maintenance window starts. (Inclusive.)
     * 
     * @param unit Unit in which to return the time.
     * @return Time in milliseconds since the beginning of the world.
     */
    public long getStart(final TimeUnit unit) {
        return unit.convert(this.start, MaintenanceWindow.DEFAULT_TIME_UNIT);
    }

    /**
     * Whether or not the give time is inside the window.
     * 
     * @param time Time to check for.
     * @param unit The unit of the provided time.
     * @return
     */
    public boolean isInside(final long time, final TimeUnit unit) {
        final long actualTime = MaintenanceWindow.DEFAULT_TIME_UNIT.convert(time, unit);
        if (this.start > actualTime) {
            return false; // window didn't start yet
        }
        if (this.end < actualTime) {
            return false; // window is already over
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MaintenanceWindow [start=").append(this.start).append(", end=")
                .append(this.end).append(", section=").append(super.toString()).append("]");
        return builder.toString();
    }

}

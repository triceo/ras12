package org.drools.planner.examples.ras2012.model;

import java.util.concurrent.TimeUnit;

/**
 * A maintenance window is a period of time when no {@link Train} is allowed to enter the part of the {@link Route} delimited by
 * the east and the west {@link Node}.
 */
public class MaintenanceWindow extends Section {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final long            start;
    private final long            end;

    /**
     * Create new instance. The order of times is not significant, they will be ordered automatically.
     * 
     * @param westNode The maintenance window starts here from the west.
     * @param eastNode The maintenance window ends here from the west.
     * @param time1 Start or end of the maintenance window in milliseconds since beginning of planning horizon.
     * @param time2 Start or end of the maintenance window in milliseconds since beginning of planning horizon.
     */
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
     * Whether or not the give time falls inside the window.
     * 
     * @param time Time to check for.
     * @param unit The unit of the provided time.
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

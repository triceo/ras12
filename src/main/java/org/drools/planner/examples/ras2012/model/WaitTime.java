package org.drools.planner.examples.ras2012.model;

public class WaitTime {

    private final int minutesWaitFor;

    public WaitTime(final int minutes) {
        if (minutes < 1) {
            throw new IllegalArgumentException("Wait time must be bigger than zero.");
        }
        this.minutesWaitFor = minutes;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final WaitTime other = (WaitTime) obj;
        if (this.minutesWaitFor != other.minutesWaitFor) {
            return false;
        }
        return true;
    }

    public int getMinutesWaitFor() {
        return this.minutesWaitFor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.minutesWaitFor;
        return result;
    }

    @Override
    public String toString() {
        return "WaitTime [minutesWaitFor=" + this.minutesWaitFor + "]";
    }

}

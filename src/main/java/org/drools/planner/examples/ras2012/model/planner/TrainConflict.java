package org.drools.planner.examples.ras2012.model.planner;

import java.math.BigDecimal;

public class TrainConflict {

    private final BigDecimal time;

    private final int        numConflicts;

    public TrainConflict(final BigDecimal time, final int numConflicts) {
        this.time = time;
        this.numConflicts = numConflicts;
    }

    public int getNumConflicts() {
        return this.numConflicts;
    }

    public BigDecimal getTime() {
        return this.time;
    }

    @Override
    public String toString() {
        return "TrainConflict [@" + this.time + ", " + this.numConflicts + "x]";
    }

}

package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.move.factory.AbstractMoveFactory;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.WaitTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitTimeAssignmentMoveFactory extends AbstractMoveFactory {

    private static final Logger logger      = LoggerFactory
                                                    .getLogger(WaitTimeAssignmentMoveFactory.class);

    /**
     * Numbers from 0 to this will all become wait times. This is done so that the algorithm has enough space for fine-tuning
     * the results.
     */
    private static final int    ALL_FIRST_X = 10;

    /**
     * Specifies what change there will be between two consecutive wait times. Please keep it between 0 and 1, both exclusive.
     * 
     * The current value has been carefully benchmarked against many other values and found to bring the best results.
     */
    private static final float  DECREASE_TO = 4.0f / 5.0f;

    private static List<WaitTime> getAllowedWaitTimes(final long horizon) {
        final List<WaitTime> waitTimes = new LinkedList<WaitTime>();
        int waitTime = (int) horizon;
        while (waitTime > WaitTimeAssignmentMoveFactory.ALL_FIRST_X) {
            waitTimes.add(WaitTime.getWaitTime(waitTime));
            waitTime = Math.round(waitTime * WaitTimeAssignmentMoveFactory.DECREASE_TO);
        }
        for (int i = WaitTimeAssignmentMoveFactory.ALL_FIRST_X; i > 0; i--) {
            waitTimes.add(WaitTime.getWaitTime(i));
        }
        waitTimes.add(null);
        WaitTimeAssignmentMoveFactory.logger
                .info("Minutes of wait time will multiply by {}, starting with {} and until {} is reached, from where they will decrease by one.",
                        new Object[] { WaitTimeAssignmentMoveFactory.DECREASE_TO, horizon,
                                WaitTimeAssignmentMoveFactory.ALL_FIRST_X });
        WaitTimeAssignmentMoveFactory.logger
                .debug("Generating moves with the following wait times: " + waitTimes);
        return waitTimes;
    }

    private final Map<Long, List<WaitTime>> waitTimes = new HashMap<Long, List<WaitTime>>();

    @Override
    public List<Move> createMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        final ProblemSolution sol = (ProblemSolution) solution;
        final long horizon = sol.getPlanningHorizon(TimeUnit.MINUTES);
        // enumerate every possible wait time value
        if (!this.waitTimes.containsKey(horizon)) {
            this.waitTimes.put(horizon, WaitTimeAssignmentMoveFactory.getAllowedWaitTimes(horizon));
        }
        // TODO estimate maximum necessary wait time from the longest arc and slowest train
        final List<Move> moves = new ArrayList<Move>();
        for (final ItineraryAssignment ia : sol.getAssignments()) {
            // when train entered X minutes after start of world, don't generate wait times to cover those X minutes.
            final long maxPlanningHorizon = horizon - ia.getTrain().getEntryTime(TimeUnit.MINUTES);
            for (final Node waitPoint : ia.getRoute().getProgression().getWaitPoints()) {
                for (final WaitTime wt : this.waitTimes.get(horizon)) {
                    if (wt == null || wt.getWaitFor(TimeUnit.MINUTES) <= maxPlanningHorizon) {
                        moves.add(new WaitTimeAssignmentMove(ia.getTrain(), ia.getRoute(),
                                waitPoint, wt));
                    }
                }
            }
        }
        return moves;
    }
}

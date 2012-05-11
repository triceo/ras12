package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.move.factory.AbstractMoveFactory;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.WaitTime;

public class WaitTimeAssignmentMoveFactory extends AbstractMoveFactory {

    /**
     * Numbers from 0 to this will all become wait times. This is done so that the algorithm has enough space for fine-tuning
     * the results.
     */
    private static final int   ALL_FIRST_X = 10;

    /**
     * Specifies what change there will be between two consecutive wait times. Please keep it between 0 and 1, both exclusive.
     */
    private static final float DECREASE_TO = 6.0f / 7.0f;

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
        return waitTimes;
    }

    @Override
    public List<Move> createMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        final RAS2012Solution sol = (RAS2012Solution) solution;
        final long horizon = sol.getPlanningHorizon(TimeUnit.MINUTES);
        // enumerate every possible wait time value
        final List<WaitTime> waitTimes = WaitTimeAssignmentMoveFactory.getAllowedWaitTimes(horizon);
        // TODO estimate maximum necessary wait time from the longest arc and slowest train
        final List<Move> moves = new ArrayList<Move>();
        for (final ItineraryAssignment ia : sol.getAssignments()) {
            // when train entered X minutes after start of world, don't generate wait times to cover those X minutes.
            final long maxPlanningHorizon = horizon - ia.getTrain().getEntryTime(TimeUnit.MINUTES);
            for (final Node waitPoint : ia.getRoute().getProgression().getWaitPoints()) {
                for (final WaitTime wt : waitTimes) {
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

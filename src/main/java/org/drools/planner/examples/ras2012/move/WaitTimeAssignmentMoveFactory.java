package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.move.factory.AbstractMoveFactory;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class WaitTimeAssignmentMoveFactory extends AbstractMoveFactory {

    @Override
    public List<Move> createMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        // TODO estimate maximum necessary wait time from the longest arc and slowest train
        final List<Move> moves = new ArrayList<Move>();
        final RAS2012Solution sol = (RAS2012Solution) solution;
        for (final ItineraryAssignment ia : sol.getAssignments()) {
            // when train entered X minutes after start of world, don't generate wait times to cover those X minutes.
            final long planningHorizon = RAS2012Solution.getPlanningHorizon(TimeUnit.MINUTES)
                    - ia.getTrain().getEntryTime(TimeUnit.MINUTES);
            final int allFirstX = 10;
            for (final Node waitPoint : ia.getRoute().getWaitPoints()) {
                moves.add(new WaitTimeAssignmentMove(ia, waitPoint, null));
                int i = 1;
                for (; i < allFirstX; i++) { // plan to the minute
                    moves.add(new WaitTimeAssignmentMove(ia, waitPoint, WaitTime.getWaitTime(i)));
                }
                for (i = allFirstX; i < planningHorizon / 8; i += 5) {
                    moves.add(new WaitTimeAssignmentMove(ia, waitPoint, WaitTime.getWaitTime(i)));
                }
                for (; i < planningHorizon / 4; i += 10) {
                    moves.add(new WaitTimeAssignmentMove(ia, waitPoint, WaitTime.getWaitTime(i)));
                }
                for (; i < planningHorizon / 2; i += 20) {
                    moves.add(new WaitTimeAssignmentMove(ia, waitPoint, WaitTime.getWaitTime(i)));
                }
                for (; i <= planningHorizon; i += 30) {
                    moves.add(new WaitTimeAssignmentMove(ia, waitPoint, WaitTime.getWaitTime(i)));
                }
            }
        }
        return moves;
    }
}

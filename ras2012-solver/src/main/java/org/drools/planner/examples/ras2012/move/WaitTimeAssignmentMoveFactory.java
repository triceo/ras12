package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.List;

import org.drools.planner.core.heuristic.selector.move.factory.MoveListFactory;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.WaitTimeAssignment;

public class WaitTimeAssignmentMoveFactory implements MoveListFactory {

    @Override
    public List<Move> createMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        final ProblemSolution sol = (ProblemSolution) solution;
        // TODO estimate maximum necessary wait time from the longest arc and slowest train
        final List<Move> moves = new ArrayList<Move>();
        for (final ItineraryAssignment ia : sol.getAssignments()) {
            for (final WaitTimeAssignment wt : ia.getWaitTimeAssignments()) {
                moves.add(new WaitTimeAssignmentMove(ia.getTrain(), ia.getRoute(), wt.getNode(), wt
                        .getWaitTime()));
            }
        }
        return moves;
    }
}

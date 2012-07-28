package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.List;

import org.drools.planner.core.heuristic.selector.move.factory.MoveListFactory;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

public class RouteReassignmentMoveFactory implements MoveListFactory {

    @Override
    public List<Move> createMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        final List<Move> moves = new ArrayList<Move>();
        final ProblemSolution sol = (ProblemSolution) solution;
        for (final Train t : sol.getTrains()) {
            for (final Route r : sol.getTerritory().getRoutes(t)) {
                moves.add(new RouteReassignmentMove(t, r));
            }
        }
        return moves;
    }

}

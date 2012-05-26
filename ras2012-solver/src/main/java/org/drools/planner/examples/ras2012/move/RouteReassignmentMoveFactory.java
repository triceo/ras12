package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.List;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.move.factory.CachedMoveFactory;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

public class RouteReassignmentMoveFactory extends CachedMoveFactory {

    @Override
    public List<Move> createCachedMoveList(@SuppressWarnings("rawtypes") final Solution solution) {
        final List<Move> moves = new ArrayList<Move>();
        final RAS2012Solution sol = (RAS2012Solution) solution;
        for (final Train t : sol.getTrains()) {
            for (final Route r : sol.getTerritory().getRoutes(t)) {
                moves.add(new RouteReassignmentMove(t, r));
            }
        }
        return moves;
    }

}

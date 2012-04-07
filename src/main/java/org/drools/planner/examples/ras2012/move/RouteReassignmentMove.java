package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.planner.ItineraryAssignment;

public class RouteReassignmentMove implements Move {

    private final ItineraryAssignment assignment;

    private final Route               route, previousRoute;

    public RouteReassignmentMove(final ItineraryAssignment ia, final Route r) {
        this.assignment = ia;
        this.route = r;
        this.previousRoute = ia.getRoute();
    }

    @Override
    public Move createUndoMove(final ScoreDirector scoreDirector) {
        return new RouteReassignmentMove(this.assignment, this.previousRoute);
    }

    @Override
    public void doMove(final ScoreDirector scoreDirector) {
        this.assignment.setRoute(this.route);
    }

    @Override
    public Collection<? extends Object> getPlanningEntities() {
        return Collections.singletonList(this.assignment);
    }

    @Override
    public Collection<? extends Object> getPlanningValues() {
        return Collections.singletonList(this.route);
    }

    @Override
    public boolean isMoveDoable(final ScoreDirector scoreDirector) {
        return this.route.isPossibleForTrain(this.assignment.getTrain());
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RouteReassignmentMove [train=");
        builder.append(this.assignment.getTrain().getName());
        builder.append(", ");
        builder.append(this.previousRoute.getId());
        builder.append(" -> ");
        builder.append(this.route.getId());
        builder.append("]");
        return builder.toString();
    }

}

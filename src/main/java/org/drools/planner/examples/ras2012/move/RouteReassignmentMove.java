package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.planner.ItineraryAssignment;

public class RouteReassignmentMove implements Move {

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RouteReassignmentMove [train=");
        builder.append(this.assignment.getTrain().getName());
        builder.append(", ");
        builder.append(previousRoute.getId());
        builder.append(" -> ");
        builder.append(route.getId());
        builder.append("]");
        return builder.toString();
    }

    private final ItineraryAssignment assignment;
    private final Route               route, previousRoute;

    public RouteReassignmentMove(ItineraryAssignment ia, Route r) {
        this.assignment = ia;
        this.route = r;
        this.previousRoute = ia.getRoute();
    }

    @Override
    public boolean isMoveDoable(ScoreDirector scoreDirector) {
        return (this.route.isPossibleForTrain(assignment.getTrain()));
    }

    @Override
    public Move createUndoMove(ScoreDirector scoreDirector) {
        return new RouteReassignmentMove(assignment, previousRoute);
    }

    @Override
    public void doMove(ScoreDirector scoreDirector) {
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

}

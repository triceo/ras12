package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.planner.examples.ras2012.model.ItineraryAssignment;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class RouteReassignmentMove implements Move {

    private final ItineraryAssignment assignment;
    private final Map<Node, WaitTime> previousWaitTimes;
    private final Route               route, previousRoute;

    public RouteReassignmentMove(final ItineraryAssignment ia, final Route r) {
        this.assignment = ia;
        this.route = r;
        this.previousRoute = ia.getRoute();
        this.previousWaitTimes = ia.getItinerary().getAllWaitTimes();
    }

    @Override
    public Move createUndoMove(final ScoreDirector scoreDirector) {
        return new RouteReassignmentUndoMove(this.assignment, this.route, this.previousRoute,
                this.previousWaitTimes);
    }

    @Override
    public void doMove(final ScoreDirector scoreDirector) {
        this.assignment.setRoute(this.route);
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
        final RouteReassignmentMove other = (RouteReassignmentMove) obj;
        if (this.assignment == null) {
            if (other.assignment != null) {
                return false;
            }
        } else if (!this.assignment.equals(other.assignment)) {
            return false;
        }
        if (this.route == null) {
            if (other.route != null) {
                return false;
            }
        } else if (!this.route.equals(other.route)) {
            return false;
        }
        return true;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.assignment == null ? 0 : this.assignment.hashCode());
        result = prime * result + (this.route == null ? 0 : this.route.hashCode());
        return result;
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

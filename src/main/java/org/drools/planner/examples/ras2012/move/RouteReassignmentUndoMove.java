package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class RouteReassignmentUndoMove implements Move {

    private final ItineraryAssignment assignment;
    private final Map<Node, WaitTime> originalWaitTimes;
    private final Route               originalRoute, routeToUndo;

    public RouteReassignmentUndoMove(final ItineraryAssignment ia, final Route routeToUndo,
            final Route originalRoute, final Map<Node, WaitTime> originalWaitTimes) {
        this.assignment = ia;
        this.originalRoute = originalRoute;
        this.routeToUndo = routeToUndo;
        this.originalWaitTimes = originalWaitTimes;
    }

    @Override
    public Move createUndoMove(final ScoreDirector scoreDirector) {
        // this is just an undo move; no need to undo an undo
        return null;
    }

    @Override
    public void doMove(final ScoreDirector scoreDirector) {
        this.assignment.setRoute(this.originalRoute);
        for (final Map.Entry<Node, WaitTime> entry : this.originalWaitTimes.entrySet()) {
            this.assignment.getItinerary().setWaitTime(entry.getValue(), entry.getKey());
        }
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
        final RouteReassignmentUndoMove other = (RouteReassignmentUndoMove) obj;
        if (this.assignment == null) {
            if (other.assignment != null) {
                return false;
            }
        } else if (!this.assignment.equals(other.assignment)) {
            return false;
        }
        if (this.originalRoute == null) {
            if (other.originalRoute != null) {
                return false;
            }
        } else if (!this.originalRoute.equals(other.originalRoute)) {
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
        return Collections.singletonList(this.routeToUndo);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.assignment == null ? 0 : this.assignment.hashCode());
        result = prime * result + (this.originalRoute == null ? 0 : this.originalRoute.hashCode());
        return result;
    }

    @Override
    public boolean isMoveDoable(final ScoreDirector scoreDirector) {
        // this is just an undo move; it should always be doable
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RouteReassignmentUndoMove [train=");
        builder.append(this.assignment.getTrain().getName());
        builder.append(", ");
        builder.append(this.routeToUndo.getId());
        builder.append(" -> ");
        builder.append(this.originalRoute.getId());
        builder.append("]");
        return builder.toString();
    }

}

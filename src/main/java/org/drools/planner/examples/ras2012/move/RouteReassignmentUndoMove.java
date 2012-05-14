package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.model.original.WaitTime;

public class RouteReassignmentUndoMove implements Move {

    private ItineraryAssignment       assignment;
    private final Train               train;
    private final Map<Node, WaitTime> originalWaitTimes;
    private final Route               originalRoute, routeToUndo;

    public RouteReassignmentUndoMove(final Train train, final Route routeToUndo,
            final Route originalRoute, final Map<Node, WaitTime> originalWaitTimes) {
        this.train = train;
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
        this.initializeMove(scoreDirector);
        scoreDirector.beforeVariableChanged(this.assignment, "route");
        this.assignment.setRoute(this.originalRoute);
        for (final Map.Entry<Node, WaitTime> entry : this.originalWaitTimes.entrySet()) {
            this.assignment.getItinerary().setWaitTime(entry.getKey(), entry.getValue());
        }
        scoreDirector.afterVariableChanged(this.assignment, "route");
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RouteReassignmentUndoMove)) {
            return false;
        }
        final RouteReassignmentUndoMove other = (RouteReassignmentUndoMove) obj;
        if (this.routeToUndo == null) {
            if (other.routeToUndo != null) {
                return false;
            }
        } else if (!this.routeToUndo.equals(other.routeToUndo)) {
            return false;
        }
        if (this.train == null) {
            if (other.train != null) {
                return false;
            }
        } else if (!this.train.equals(other.train)) {
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
        result = prime * result + (this.routeToUndo == null ? 0 : this.routeToUndo.hashCode());
        result = prime * result + (this.train == null ? 0 : this.train.hashCode());
        return result;
    }

    private ItineraryAssignment initializeMove(final ScoreDirector scoreDirector) {
        this.assignment = ((RAS2012Solution) scoreDirector.getWorkingSolution())
                .getAssignment(this.train);
        return this.assignment;
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
        builder.append(this.train.getName());
        builder.append(", ");
        builder.append(this.routeToUndo.getId());
        builder.append(" -> ");
        builder.append(this.originalRoute.getId());
        builder.append("]");
        return builder.toString();
    }

}

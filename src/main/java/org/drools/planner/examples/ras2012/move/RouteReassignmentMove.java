package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class RouteReassignmentMove implements Move {

    private ItineraryAssignment assignment;
    private final Train         train;
    private Map<Node, WaitTime> previousWaitTimes;
    private final Route         route;
    private Route               previousRoute;

    public RouteReassignmentMove(final Train t, final Route r) {
        this.train = t;
        this.route = r;
    }

    @Override
    public Move createUndoMove(final ScoreDirector scoreDirector) {
        this.initializeMove(scoreDirector);
        return new RouteReassignmentUndoMove(this.train, this.route, this.previousRoute,
                this.previousWaitTimes);
    }

    @Override
    public void doMove(final ScoreDirector scoreDirector) {
        this.assignment = this.initializeMove(scoreDirector);
        scoreDirector.beforeVariableChanged(this.assignment, "route");
        this.assignment.setRoute(this.route);
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
        if (!(obj instanceof RouteReassignmentMove)) {
            return false;
        }
        final RouteReassignmentMove other = (RouteReassignmentMove) obj;
        if (this.route != other.route) {
            return false;
        }
        if (this.train != other.train) {
            return false;
        }
        return true;
    }

    @Override
    public Collection<? extends Object> getPlanningEntities() {
        if (this.assignment == null) {
            throw new IllegalStateException("Move not yet initialized!");
        }
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
        result = prime * result + (this.route == null ? 0 : this.route.hashCode());
        result = prime * result + (this.train == null ? 0 : this.train.hashCode());
        return result;
    }

    private ItineraryAssignment initializeMove(final ScoreDirector scoreDirector) {
        this.assignment = ((RAS2012Solution) scoreDirector.getWorkingSolution())
                .getAssignment(this.train);
        this.previousRoute = this.assignment.getRoute();
        this.previousWaitTimes = this.assignment.getItinerary().getWaitTimes();
        return this.assignment;
    }

    @Override
    public boolean isMoveDoable(final ScoreDirector scoreDirector) {
        this.initializeMove(scoreDirector);
        return this.route != this.previousRoute;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RouteReassignmentMove [train=");
        builder.append(this.train.getName());
        builder.append(", ");
        builder.append(this.previousRoute.getId());
        builder.append(" -> ");
        builder.append(this.route.getId());
        builder.append("]");
        return builder.toString();
    }

}

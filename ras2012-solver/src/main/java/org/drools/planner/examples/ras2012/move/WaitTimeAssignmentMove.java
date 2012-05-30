package org.drools.planner.examples.ras2012.move;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class WaitTimeAssignmentMove implements Move {

    private ItineraryAssignment assignment;
    private final Train         train;
    private final Route         route;
    private final Node          node;
    private final WaitTime      waitTime;
    private WaitTime            previousWaitTime;

    public WaitTimeAssignmentMove(final Train t, final Route r, final Node n, final WaitTime wt) {
        this.train = t;
        this.route = r;
        this.node = n;
        this.waitTime = wt;
    }

    private boolean assignmentExists(final ScoreDirector scoreDirector) {
        return this.getAssignment(scoreDirector) != null;
    }

    @Override
    public Move createUndoMove(final ScoreDirector scoreDirector) {
        this.initializeMove(scoreDirector);
        return new WaitTimeAssignmentMove(this.train, this.route, this.node, this.previousWaitTime);
    }

    @Override
    public void doMove(final ScoreDirector scoreDirector) {
        this.initializeMove(scoreDirector);
        final ItineraryAssignment ia = this.getAssignment(scoreDirector);
        scoreDirector.beforeVariableChanged(ia, "waitTime");
        ia.getItinerary().setWaitTime(this.node, this.waitTime);
        scoreDirector.afterVariableChanged(ia, "waitTime");
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WaitTimeAssignmentMove)) {
            return false;
        }
        final WaitTimeAssignmentMove other = (WaitTimeAssignmentMove) obj;
        if (this.node != other.node) {
            return false;
        }
        if (this.waitTime != other.waitTime) {
            return false;
        }
        if (this.route != other.route) {
            return false;
        }
        if (this.train != other.train) {
            return false;
        }
        return true;
    }

    private ItineraryAssignment getAssignment(final ScoreDirector scoreDirector) {
        return this.getSolution(scoreDirector).getAssignment(this.train);
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
        return Collections.singletonList(this.waitTime);
    }

    private ProblemSolution getSolution(final ScoreDirector scoreDirector) {
        return (ProblemSolution) scoreDirector.getWorkingSolution();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.node == null ? 0 : this.node.hashCode());
        result = prime * result + (this.route == null ? 0 : this.route.hashCode());
        result = prime * result + (this.train == null ? 0 : this.train.hashCode());
        result = prime * result + (this.waitTime == null ? 0 : this.waitTime.hashCode());
        return result;
    }

    private ItineraryAssignment initializeMove(final ScoreDirector scoreDirector) {
        this.assignment = this.getAssignment(scoreDirector);
        this.previousWaitTime = this.assignment.getItinerary().getWaitTime(this.node);
        return this.assignment;
    }

    @Override
    public boolean isMoveDoable(final ScoreDirector scoreDirector) {
        if (!this.assignmentExists(scoreDirector)) {
            return false;
        }
        this.initializeMove(scoreDirector);
        if (this.assignment.getRoute() != this.route) {
            return false;
        }
        if (this.assignment.getItinerary().isNodeOnRoute(this.node)) {
            return this.waitTime != this.previousWaitTime;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("WaitTimeAssignmentMove [");
        builder.append(this.train.getName());
        builder.append("@");
        builder.append(this.route.getId());
        builder.append("-");
        builder.append(this.node.getId());
        builder.append(", ");
        if (this.previousWaitTime == null) {
            builder.append(0);
        } else {
            builder.append(this.previousWaitTime.getWaitFor(TimeUnit.MINUTES));
        }
        builder.append("->");
        if (this.waitTime == null) {
            builder.append(0);
        } else {
            builder.append(this.waitTime.getWaitFor(TimeUnit.MINUTES));
        }
        builder.append("]");
        return builder.toString();
    }

}

package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.drools.planner.core.heuristic.selector.move.factory.MoveIteratorFactory;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.WaitTimeAssignment;
import org.drools.planner.examples.ras2012.util.RandomAccessor;

public class WaitTimeAssignmentMoveFactory implements MoveIteratorFactory {

    private static final class RandomWaitTimeAssignmentMoveIterator implements Iterator<Move> {

        private final Map<ItineraryAssignment, RandomAccessor<WaitTimeAssignment>> waitTimes = new HashMap<>();
        private final RandomAccessor<ItineraryAssignment>                          itineraries;

        public RandomWaitTimeAssignmentMoveIterator(final ProblemSolution solution,
                final Random random) {
            this.itineraries = new RandomAccessor<ItineraryAssignment>(solution.getAssignments(),
                    random);
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                this.waitTimes
                        .put(ia, new RandomAccessor<WaitTimeAssignment>(
                                ia.getWaitTimeAssignments(), random));
            }
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Move next() {
            final ItineraryAssignment ia = this.itineraries.get();
            final WaitTimeAssignment wta = this.waitTimes.get(ia).get();
            return new WaitTimeAssignmentMove(ia.getTrain(), ia.getRoute(), wta.getNode(),
                    wta.getWaitTime());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static final class WaitTimeAssignmentMoveIterator implements Iterator<Move> {

        private final List<Pair<ItineraryAssignment, WaitTimeAssignment>> pairs        = new ArrayList<>();
        private int                                                       currentIndex = 0;

        public WaitTimeAssignmentMoveIterator(final ProblemSolution solution) {
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                for (final WaitTimeAssignment wt : ia.getWaitTimeAssignments()) {
                    this.pairs.add(Pair.of(ia, wt));
                }
            }
        }

        @Override
        public boolean hasNext() {
            return this.currentIndex < this.pairs.size() - 1;
        }

        @Override
        public Move next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            final Pair<ItineraryAssignment, WaitTimeAssignment> pair = this.pairs
                    .get(this.currentIndex++);
            final ItineraryAssignment ia = pair.getLeft();
            final WaitTimeAssignment wta = pair.getRight();
            return new WaitTimeAssignmentMove(ia.getTrain(), ia.getRoute(), wta.getNode(),
                    wta.getWaitTime());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public Iterator<Move> createOriginalMoveIterator(final ScoreDirector arg0) {
        return new WaitTimeAssignmentMoveIterator((ProblemSolution) arg0.getWorkingSolution());
    }

    @Override
    public Iterator<Move> createRandomMoveIterator(final ScoreDirector arg0, final Random arg1) {
        return new RandomWaitTimeAssignmentMoveIterator(
                (ProblemSolution) arg0.getWorkingSolution(), arg1);
    }

    @Override
    public long getSize(final ScoreDirector arg0) {
        final ProblemSolution sol = (ProblemSolution) arg0.getWorkingSolution();
        long size = 0;
        for (final ItineraryAssignment ia : sol.getAssignments()) {
            size += ia.getWaitTimeAssignments().size();
        }
        return size;
    }
}

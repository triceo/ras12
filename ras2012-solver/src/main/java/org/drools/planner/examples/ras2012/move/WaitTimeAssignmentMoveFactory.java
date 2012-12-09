package org.drools.planner.examples.ras2012.move;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.drools.planner.core.heuristic.selector.move.factory.MoveIteratorFactory;
import org.drools.planner.core.move.Move;
import org.drools.planner.core.score.director.ScoreDirector;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.WaitTimeAssignment;

public class WaitTimeAssignmentMoveFactory implements MoveIteratorFactory {

    private static final class RandomWaitTimeAssignmentMoveIterator implements Iterator<Move> {

        private final List<Pair<ItineraryAssignment, WaitTimeAssignment>> pairs       = new ArrayList<>();
        private final Random                                              random;
        private final Set<Integer>                                        usedIndices = new TreeSet<>();

        public RandomWaitTimeAssignmentMoveIterator(final ProblemSolution solution,
                final Random random) {
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                for (final WaitTimeAssignment wt : ia.getWaitTimeAssignments()) {
                    this.pairs.add(Pair.of(ia, wt));
                }
            }
            this.random = random;
        }

        @Override
        public boolean hasNext() {
            return this.usedIndices.size() < this.pairs.size();
        }

        @Override
        public Move next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            int index = -1;
            do {
                index = this.random.nextInt(this.pairs.size());
            } while (this.usedIndices.contains(index));
            this.usedIndices.add(index);
            final Pair<ItineraryAssignment, WaitTimeAssignment> pair = this.pairs.get(index);
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

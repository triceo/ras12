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
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;

public class RouteReassignmentMoveFactory implements MoveIteratorFactory {

    private static final class RandomRouteReassignmentMoveIterator implements Iterator<Move> {

        private final List<Pair<Train, Route>> pairs       = new ArrayList<>();
        private final Random                   random;
        private final Set<Integer>             usedIndices = new TreeSet<>();

        public RandomRouteReassignmentMoveIterator(final ProblemSolution solution,
                final Random random) {
            for (final Train t : solution.getTrains()) {
                for (final Route r : solution.getTerritory().getRoutes(t)) {
                    this.pairs.add(Pair.of(t, r));
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
            final Pair<Train, Route> pair = this.pairs.get(index);
            return new RouteReassignmentMove(pair.getLeft(), pair.getRight());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static final class RouteReassignmentMoveIterator implements Iterator<Move> {

        private final List<Pair<Train, Route>> pairs        = new ArrayList<>();
        private int                            currentIndex = 0;

        public RouteReassignmentMoveIterator(final ProblemSolution solution) {
            for (final Train t : solution.getTrains()) {
                for (final Route r : solution.getTerritory().getRoutes(t)) {
                    this.pairs.add(Pair.of(t, r));
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
            final Pair<Train, Route> pair = this.pairs.get(this.currentIndex++);
            return new RouteReassignmentMove(pair.getLeft(), pair.getRight());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public Iterator<Move> createOriginalMoveIterator(final ScoreDirector arg0) {
        return new RouteReassignmentMoveIterator((ProblemSolution) arg0.getWorkingSolution());
    }

    @Override
    public Iterator<Move> createRandomMoveIterator(final ScoreDirector arg0, final Random arg1) {
        return new RandomRouteReassignmentMoveIterator((ProblemSolution) arg0.getWorkingSolution(),
                arg1);
    }

    @Override
    public long getSize(final ScoreDirector arg0) {
        final ProblemSolution sol = (ProblemSolution) arg0.getWorkingSolution();
        long size = 0;
        for (final Train t : sol.getTrains()) {
            size += sol.getTerritory().getRoutes(t).size();
        }
        return size;
    }

}

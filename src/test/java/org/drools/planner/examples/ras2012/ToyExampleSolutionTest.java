package org.drools.planner.examples.ras2012;

import java.io.File;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.model.original.WaitTime;
import org.junit.Before;
import org.junit.Test;

public class ToyExampleSolutionTest {

    private final RAS2012Solution SOLUTION = new RAS2012ProblemIO()
                                                   .read(new File(
                                                           "src/main/resources/org/drools/planner/examples/ras2012/TOY.txt"));

    private long getArrivalAtNode(final Itinerary i, final Node n) {
        for (final SortedMap.Entry<Long, Node> entry : i.getSchedule().entrySet()) {
            if (entry.getValue() == n) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Node not found!");
    }

    private HardAndSoftScore getScoreForSolution(final RAS2012Solution solution) {
        final RAS2012ScoreCalculator calc = new RAS2012ScoreCalculator();
        calc.resetWorkingSolution(solution);
        return calc.calculateScore();
    }

    private Route locateRoute(final int id) {
        for (final Route r : this.SOLUTION.getNetwork().getAllRoutes()) {
            if (r.getId() == id) {
                return r;
            }
        }
        throw new IllegalArgumentException("Route does not exist!");
    }

    private Train locateTrain(final String name) {
        for (final Train t : this.SOLUTION.getTrains()) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Train does not exist!");
    }

    @Before
    public void prepareSolution() {
        // prepare B1 according to the example
        final Train b1 = this.locateTrain("B1");
        final ItineraryAssignment b1Assignment = this.SOLUTION.getAssignment(b1);
        b1Assignment.setRoute(this.locateRoute(45));
        // we must be at node 6 precisely at 5110576
        long delay = 5110576 - this.getArrivalAtNode(b1Assignment.getItinerary(), Node.getNode(6));
        b1Assignment.getItinerary().setWaitTime(WaitTime.getWaitTime(delay, TimeUnit.MILLISECONDS),
                Node.getNode(9));
        // prepare C1 according to the example
        final Train c1 = this.locateTrain("C1");
        final ItineraryAssignment c1Assignment = this.SOLUTION.getAssignment(c1);
        c1Assignment.setRoute(this.locateRoute(9));
        // we must be at node 6 precisely at 7713668
        delay = 7713668 - this.getArrivalAtNode(c1Assignment.getItinerary(), Node.getNode(6));
        c1Assignment.getItinerary().setWaitTime(WaitTime.getWaitTime(delay, TimeUnit.MILLISECONDS),
                Node.getNode(4));
        // prepare A1 according to the example
        final Train a1 = this.locateTrain("A1");
        final ItineraryAssignment a1Assignment = this.SOLUTION.getAssignment(a1);
        a1Assignment.setRoute(this.locateRoute(20));
    }

    @Test
    public void test() {
        final HardAndSoftScore score = this.getScoreForSolution(this.SOLUTION);
        final HardAndSoftScore expectedScore = DefaultHardAndSoftScore.valueOf(100, -925);
        Assert.assertEquals(expectedScore, score);
    }
}

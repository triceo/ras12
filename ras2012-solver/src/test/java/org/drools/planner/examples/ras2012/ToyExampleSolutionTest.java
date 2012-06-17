package org.drools.planner.examples.ras2012;

import java.io.File;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;
import org.drools.planner.examples.ras2012.util.SolutionIO;
import org.junit.Before;
import org.junit.Test;

public class ToyExampleSolutionTest {

    private final ProblemSolution SOLUTION  = new SolutionIO()
                                                    .read(new File(
                                                            "src/main/resources/org/drools/planner/examples/ras2012/TOY.txt"));

    private static final File     DIRECTORY = new File("data/tests/"
                                                    + ToyExampleSolutionTest.class.getName());

    private long getArrivalAtNode(final Itinerary i, final Node n) {
        for (final SortedMap.Entry<Long, Node> entry : i.getSchedule().entrySet()) {
            if (entry.getValue() == n) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Node not found!");
    }

    private HardAndSoftScore getScoreForSolution(final ProblemSolution solution) {
        final ScoreCalculator calc = new ScoreCalculator();
        calc.resetWorkingSolution(solution);
        return calc.calculateScore();
    }

    private Route locateRoute(final int id) {
        for (final Route r : this.SOLUTION.getTerritory().getAllRoutes()) {
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
        ToyExampleSolutionTest.DIRECTORY.mkdirs();
        // prepare B1 according to the example
        final Train b1 = this.locateTrain("B1");
        final ItineraryAssignment b1Assignment = this.SOLUTION.getAssignment(b1);
        b1Assignment.setRoute(this.locateRoute(5));
        // we must be at node 6 precisely at 5021076
        long delay = 5316076 - this.getArrivalAtNode(b1Assignment.getItinerary(), Node.getNode(6));
        b1Assignment.getItinerary().setWaitTime(Node.getNode(9),
                WaitTime.getWaitTime(delay, TimeUnit.MILLISECONDS));
        b1Assignment.getRoute().visualize(new File(ToyExampleSolutionTest.DIRECTORY, "B1.png"));
        // prepare C1 according to the example
        final Train c1 = this.locateTrain("C1");
        final ItineraryAssignment c1Assignment = this.SOLUTION.getAssignment(c1);
        c1Assignment.setRoute(this.locateRoute(0));
        c1Assignment.getItinerary().setWaitTime(Node.getNode(0),
                WaitTime.getWaitTime(2310000, TimeUnit.MILLISECONDS));
        // we must be at node 6 precisely at 7568664
        delay = 8158664 - this.getArrivalAtNode(c1Assignment.getItinerary(), Node.getNode(6));
        c1Assignment.getItinerary().setWaitTime(Node.getNode(4),
                WaitTime.getWaitTime(delay, TimeUnit.MILLISECONDS));
        c1Assignment.getRoute().visualize(new File(ToyExampleSolutionTest.DIRECTORY, "C1.png"));
        // prepare A1 according to the example
        final Train a1 = this.locateTrain("A1");
        final ItineraryAssignment a1Assignment = this.SOLUTION.getAssignment(a1);
        a1Assignment.setRoute(this.locateRoute(4));
        a1Assignment.getRoute().visualize(new File(ToyExampleSolutionTest.DIRECTORY, "A1.png"));
        this.SOLUTION.visualize(new File(ToyExampleSolutionTest.DIRECTORY, "solution.png"));
        this.SOLUTION.getTerritory().visualize(
                new File(ToyExampleSolutionTest.DIRECTORY, "territory.png"));
    }

    @Test
    public void testScore() {
        final HardAndSoftScore score = this.getScoreForSolution(this.SOLUTION);
        Assert.assertEquals(0, score.getHardScore());
        Assert.assertEquals(-1014.0, score.getSoftScore(), 5);
    }

    @Test
    public void testTrainA1Times() {
        final Itinerary i = this.SOLUTION.getAssignment(this.locateTrain("A1")).getItinerary();
        Assert.assertEquals(1200000, i.getArrivalTime(Node.getNode(0)));
        Assert.assertEquals(2010000, i.getArrivalTime(Node.getNode(1)));
        Assert.assertEquals(2055000, i.getArrivalTime(Node.getNode(3)));
        Assert.assertEquals(2100000, i.getArrivalTime(Node.getNode(5)));
        Assert.assertEquals(3000000, i.getArrivalTime(Node.getNode(6)));
        Assert.assertEquals(3810000, i.getArrivalTime(Node.getNode(7)));
        Assert.assertEquals(3855000, i.getArrivalTime(Node.getNode(8)));
        Assert.assertEquals(3900000, i.getArrivalTime(Node.getNode(11)));
        Assert.assertEquals(4800000, i.getArrivalTime(Node.getNode(12)));
        Assert.assertEquals(2010000, i.getLeaveTime(Node.getNode(0)));
        Assert.assertEquals(2055000, i.getLeaveTime(Node.getNode(1)));
        Assert.assertEquals(2100000, i.getLeaveTime(Node.getNode(3)));
        Assert.assertEquals(3000000, i.getLeaveTime(Node.getNode(5)));
        Assert.assertEquals(3810000, i.getLeaveTime(Node.getNode(6)));
        Assert.assertEquals(3855000, i.getLeaveTime(Node.getNode(7)));
        Assert.assertEquals(3900000, i.getLeaveTime(Node.getNode(8)));
        Assert.assertEquals(4800000, i.getLeaveTime(Node.getNode(11)));
        Assert.assertEquals(-1, i.getLeaveTime(Node.getNode(12)));
    }

    @Test
    public void testTrainB1Times() {
        final Itinerary i = this.SOLUTION.getAssignment(this.locateTrain("B1")).getItinerary();
        Assert.assertEquals(1200000, i.getArrivalTime(Node.getNode(12)));
        Assert.assertEquals(2410084, i.getArrivalTime(Node.getNode(11)));
        Assert.assertEquals(2482084, i.getArrivalTime(Node.getNode(10)));
        Assert.assertEquals(4155000, i.getArrivalTime(Node.getNode(9)));
        Assert.assertEquals(4227000, i.getArrivalTime(Node.getNode(7)));
        Assert.assertEquals(5316076, i.getArrivalTime(Node.getNode(6)));
        Assert.assertEquals(6526160, i.getArrivalTime(Node.getNode(5)));
        Assert.assertEquals(6586664, i.getArrivalTime(Node.getNode(3)));
        Assert.assertEquals(6647168, i.getArrivalTime(Node.getNode(1)));
        Assert.assertEquals(7736244, i.getArrivalTime(Node.getNode(0)));
        Assert.assertEquals(2410084, i.getLeaveTime(Node.getNode(12)));
        Assert.assertEquals(2482084, i.getLeaveTime(Node.getNode(11)));
        Assert.assertEquals(4155000, i.getLeaveTime(Node.getNode(10)));
        Assert.assertEquals(4227000, i.getLeaveTime(Node.getNode(9)));
        Assert.assertEquals(5316076, i.getLeaveTime(Node.getNode(7)));
        Assert.assertEquals(6526160, i.getLeaveTime(Node.getNode(6)));
        Assert.assertEquals(6586664, i.getLeaveTime(Node.getNode(5)));
        Assert.assertEquals(6647168, i.getLeaveTime(Node.getNode(3)));
        Assert.assertEquals(7736244, i.getLeaveTime(Node.getNode(1)));
        Assert.assertEquals(-1, i.getLeaveTime(Node.getNode(0)));
    }

    @Test
    public void testTrainC1Times() {
        final Itinerary i = this.SOLUTION.getAssignment(this.locateTrain("C1")).getItinerary();
        Assert.assertEquals(2310000, i.getArrivalTime(Node.getNode(0)));
        Assert.assertEquals(3390000, i.getArrivalTime(Node.getNode(1)));
        Assert.assertEquals(3462000, i.getArrivalTime(Node.getNode(2)));
        Assert.assertEquals(6886664, i.getArrivalTime(Node.getNode(4)));
        Assert.assertEquals(6958664, i.getArrivalTime(Node.getNode(5)));
        Assert.assertEquals(8158664, i.getArrivalTime(Node.getNode(6)));
        Assert.assertEquals(9238664, i.getArrivalTime(Node.getNode(7)));
        Assert.assertEquals(9298664, i.getArrivalTime(Node.getNode(8)));
        Assert.assertEquals(9358664, i.getArrivalTime(Node.getNode(11)));
        Assert.assertEquals(10558664, i.getArrivalTime(Node.getNode(12)));
        Assert.assertEquals(3390000, i.getLeaveTime(Node.getNode(0)));
        Assert.assertEquals(3462000, i.getLeaveTime(Node.getNode(1)));
        Assert.assertEquals(6886664, i.getLeaveTime(Node.getNode(2)));
        Assert.assertEquals(6958664, i.getLeaveTime(Node.getNode(4)));
        Assert.assertEquals(8158664, i.getLeaveTime(Node.getNode(5)));
        Assert.assertEquals(9238664, i.getLeaveTime(Node.getNode(6)));
        Assert.assertEquals(9298664, i.getLeaveTime(Node.getNode(7)));
        Assert.assertEquals(9358664, i.getLeaveTime(Node.getNode(8)));
        Assert.assertEquals(10558664, i.getLeaveTime(Node.getNode(11)));
        Assert.assertEquals(-1, i.getLeaveTime(Node.getNode(12)));
    }
}

package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import junit.framework.Assert;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Arc.TrackType;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.ScheduleAdherenceRequirement;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RouteTest {

    /**
     * Run the test for both eastbound and westbound routes.
     * 
     * @return
     */
    @Parameters
    public static Collection<Object[]> getDirections() {
        final Collection<Object[]> directions = new ArrayList<Object[]>();
        directions.add(new Boolean[] { true });
        directions.add(new Boolean[] { false });
        return directions;
    }

    private final boolean isEastbound;

    public RouteTest(final boolean isEastbound) {
        this.isEastbound = isEastbound;
    }

    @Test
    public void testConstructor() {
        Assert.assertEquals("Directions should match. ", new Route(this.isEastbound).isEastbound(),
                this.isEastbound);
    }

    @Test
    public void testContains() {
        final Arc arc = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc arc2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        Route r = new Route(this.isEastbound);
        Assert.assertFalse("Empty collection shouldn't contain the arc.", r.contains(arc));
        r = r.extend(arc);
        Assert.assertTrue("Collection should now contain the arc.", r.contains(arc));
        Assert.assertFalse("Collection should not contain the never-inserted arc.",
                r.contains(arc2));
    }

    @Test
    public void testEquals() {
        final Route r = new Route(this.isEastbound);
        final Route r2 = new Route(this.isEastbound);
        Assert.assertTrue("Route should be equal to itself.", r.equals(r));
        Assert.assertFalse("Route should not equal null.", r.equals(null));
        Assert.assertFalse("No two routes should be equal.", r.equals(r2));
        Assert.assertFalse("No two routes should be equal.", r2.equals(r));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendNotWithSameArc() {
        final Arc a = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = new Route(this.isEastbound);
        final Route r2 = r.extend(a);
        r2.extend(a);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendNull() {
        final Route r = new Route(this.isEastbound);
        r.extend(null);
    }

    @Test
    public void testExtendResultsInNewRoute() {
        final Route r = new Route(this.isEastbound);
        final Route r2 = r.extend(new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node
                .getNode(1)));
        Assert.assertNotSame("Extended route should be a clone of the original one.", r2, r);
        Assert.assertFalse("Old and extended routes shouldn't be equal.", r2.equals(r));
    }

    @Test
    public void testGetInitialAndTerminalArc() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n3, n4);
        Route r = new Route(this.isEastbound).extend(a1);
        Assert.assertSame("With just one arc, initial and terminal arcs should be the same. ",
                r.getTerminalArc(), r.getInitialArc());
        Assert.assertSame("With just one arc, initial and terminal arcs should equal. ",
                r.getTerminalArc(), r.getInitialArc());
        if (r.isEastbound()) {
            r = r.extend(a2);
            Assert.assertSame("With two arcs eastbound, the first inserted one should be initial.",
                    a1, r.getInitialArc());
            Assert.assertSame(
                    "With two arcs eastbound, the second inserted one should be terminal.", a2,
                    r.getTerminalArc());
            r = r.extend(a3);
            Assert.assertSame(
                    "With three arcs eastbound, the first inserted one should be initial.", a1,
                    r.getInitialArc());
            Assert.assertSame(
                    "With three arcs eastbound, the last inserted one should be terminal.", a3,
                    r.getTerminalArc());
        } else {
            r = r.extend(a2);
            Assert.assertSame(
                    "With two arcs westbound, the second inserted one should be initial.", a2,
                    r.getInitialArc());
            Assert.assertSame(
                    "With two arcs westbound, the first inserted one should be terminal.", a1,
                    r.getTerminalArc());
            r = r.extend(a3);
            Assert.assertSame(
                    "With three arcs westbound, the last inserted one should be initial.", a3,
                    r.getInitialArc());
            Assert.assertSame(
                    "With three arcs westbound, the first inserted one should be terminal.", a1,
                    r.getTerminalArc());
        }
    }

    @Test
    public void testGetNextAndPreviousArc() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        Route r = new Route(this.isEastbound);
        r = r.extend(a1);
        Assert.assertNull("On a route with single arc, next arc to the first one is null.",
                r.getNextArc(a1));
        Assert.assertNull("On a route with single arc, previous arc to the first one is null.",
                r.getPreviousArc(a1));
        r = r.extend(a2);
        if (r.isWestbound()) {
            Assert.assertNull(
                    "On westbound route with two arcs, next arc to the first one is null.",
                    r.getNextArc(a1));
            Assert.assertSame(
                    "On westbound route with two arcs, next arc to the second one is the first.",
                    a1, r.getNextArc(a2));
            Assert.assertSame(
                    "On westbound route with two arcs, previous arc to the first one is the second.",
                    a2, r.getPreviousArc(a1));
            Assert.assertNull(
                    "On westbound route with two arcs, previous arc to the second one is null.",
                    r.getPreviousArc(a2));
        } else {
            Assert.assertSame(
                    "On eastbound route with two arcs, next arc to the first one is the second.",
                    a2, r.getNextArc(a1));
            Assert.assertNull(
                    "On eastbound route with two arcs, next arc to the second one is null.",
                    r.getNextArc(a2));
            Assert.assertNull(
                    "On eastbound route with two arcs, previous arc to the first one is null.",
                    r.getPreviousArc(a1));
            Assert.assertSame(
                    "On eastbound route with two arcs, previous arc to the second one is the first.",
                    a1, r.getPreviousArc(a2));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextArcEmptyRoute() {
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = new Route(this.isEastbound);
        r.getNextArc(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextArcInvalid() {
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = new Route(this.isEastbound).extend(a1);
        r.getNextArc(a2);
    }

    @Test
    public void testGetNextArcNull() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        Route r = new Route(this.isEastbound);
        r = r.extend(a1);
        Assert.assertSame("On a route with single arc, null next arc is the first one.",
                r.getInitialArc(), r.getNextArc(null));
        r = r.extend(a2);
        Assert.assertSame("On a route with two arcs, null next arc is still the first one.",
                r.getInitialArc(), r.getNextArc(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextArcNullEmptyRoute() {
        final Route r = new Route(this.isEastbound);
        r.getNextArc(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousArcEmptyRoute() {
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = new Route(this.isEastbound);
        r.getPreviousArc(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousArcInvalid() {
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = new Route(this.isEastbound).extend(a1);
        r.getPreviousArc(a2);
    }

    @Test
    public void testGetPreviousArcNull() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        Route r = new Route(this.isEastbound);
        r = r.extend(a1);
        Assert.assertSame("On a route with single arc, null previous arc is the first one.",
                r.getTerminalArc(), r.getPreviousArc(null));
        r = r.extend(a2);
        Assert.assertSame("On a route with two arcs, null previous arc is the last one.",
                r.getTerminalArc(), r.getPreviousArc(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousArcNullEmptyRoute() {
        final Route r = new Route(this.isEastbound);
        r.getPreviousArc(null);
    }

    @Test
    public void testGetWaitPointsOnCrossovers() {
        this.testGetWaitPointsOnSwitchesAndCrossovers(TrackType.CROSSOVER);
    }

    @Test
    public void testGetWaitPointsOnMainTracks() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.MAIN_1, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(TrackType.MAIN_2, BigDecimal.ONE, n3, n4);
        final Route r = new Route(this.isEastbound).extend(a1).extend(a2).extend(a3);
        final Collection<Node> wp = r.getWaitPoints();
        Assert.assertEquals("Only main tracks means just one wait point at the beginning.", 1,
                wp.size());
        if (r.isEastbound()) {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getWestNode()));
        } else {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getEastNode()));
        }
    }

    @Test
    public void testGetWaitPointsOnSiding() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(TrackType.SIDING, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n3, n4);
        final Route r = new Route(this.isEastbound).extend(a1).extend(a2).extend(a3);
        final Collection<Node> wp = r.getWaitPoints();
        Assert.assertEquals("One siding means two wait points, start + siding.", 2, wp.size());
        if (r.isEastbound()) {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getWestNode()));
            Assert.assertTrue("Eastbound sidings waypoint is at the east side of the siding.",
                    wp.contains(a2.getEastNode()));
        } else {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getEastNode()));
            Assert.assertTrue("Westbound sidings waypoint is at the west side of the siding.",
                    wp.contains(a2.getWestNode()));
        }
    }

    @Test
    public void testGetWaitPointsOnSwitches() {
        this.testGetWaitPointsOnSwitchesAndCrossovers(TrackType.SWITCH);
    }

    private void testGetWaitPointsOnSwitchesAndCrossovers(final TrackType t) {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(t, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n3, n4);
        final Route r = new Route(this.isEastbound).extend(a1).extend(a2).extend(a3);
        final Collection<Node> wp = r.getWaitPoints();
        Assert.assertEquals("One SW/C means two wait points, start + SW/C.", 2, wp.size());
        if (r.isEastbound()) {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getWestNode()));
            Assert.assertTrue("Eastbound SW/C waypoint is at the west side of the siding.",
                    wp.contains(a2.getWestNode()));
        } else {
            Assert.assertTrue("One of the wait points should be the route start.",
                    wp.contains(r.getInitialArc().getEastNode()));
            Assert.assertTrue("Westbound SW/C waypoint is at the east side of the siding.",
                    wp.contains(a2.getEastNode()));
        }
    }

    @Test
    public void testIsPossibleForHazardousTrain() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Arc a = new Arc(TrackType.SIDING, BigDecimal.ONE, n1, n2);
        Route r = new Route(this.isEastbound);
        r = r.extend(a);
        final boolean isWestbound = !this.isEastbound;
        final Node originNode = isWestbound ? n2 : n1;
        final Node destinationNode = isWestbound ? n1 : n2;
        final Train normalTrain = new Train("A3", BigDecimal.ONE, BigDecimal.ONE, 90, originNode,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                false, isWestbound);
        Assert.assertTrue("Train that's not hazardous won't be let through.",
                r.isPossibleForTrain(normalTrain));
        final Train hazardous = new Train("A4", BigDecimal.ONE, BigDecimal.ONE, 90, originNode,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, isWestbound);
        Assert.assertFalse("Hazardous train will be let through.", r.isPossibleForTrain(hazardous));
    }

    @Test
    public void testIsPossibleForTrainDirection() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Arc a = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        Route r = new Route(this.isEastbound);
        r = r.extend(a);
        // prepare trains
        final Train eastbound = new Train("A1", BigDecimal.ONE, BigDecimal.ONE, 90, n1, n2, 0, 1,
                0, Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train westbound = new Train("A2", BigDecimal.ONE, BigDecimal.ONE, 90, n2, n1, 0, 1,
                0, Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        if (r.isWestbound()) {
            Assert.assertTrue("Westbound train should be possible on westbound route.",
                    r.isPossibleForTrain(westbound));
            Assert.assertFalse("Eastbound train should not be possible on westbound route.",
                    r.isPossibleForTrain(eastbound));
        } else {
            Assert.assertFalse("Westbound train should not be possible on eastbound route.",
                    r.isPossibleForTrain(westbound));
            Assert.assertTrue("Eastbound train should be possible on eastbound route.",
                    r.isPossibleForTrain(eastbound));
        }
    }

    @Test
    public void testIsPossibleForTrainHeaviness() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Arc a = new Arc(TrackType.SIDING, BigDecimal.ONE, n1, n2);
        Route r = new Route(this.isEastbound);
        r = r.extend(a);
        final boolean isWestbound = !this.isEastbound;
        final Node originNode = isWestbound ? n2 : n1;
        final Node destinationNode = isWestbound ? n1 : n2;
        final Train normalTrain = new Train("A3", BigDecimal.ONE, BigDecimal.ONE, 90, originNode,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                false, isWestbound);
        Assert.assertTrue("Train that's not heavy won't be let through.",
                r.isPossibleForTrain(normalTrain));
        final Train heavyTrain = new Train("A4", BigDecimal.ONE, BigDecimal.ONE, 110, originNode,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                false, isWestbound);
        Assert.assertFalse("Heavy train will be let through.", r.isPossibleForTrain(heavyTrain));
    }

    @Test
    public void testIsPossibleForTrainLength() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Arc a = new Arc(TrackType.SIDING, BigDecimal.ONE, n1, n2);
        Route r = new Route(this.isEastbound);
        r = r.extend(a);
        final boolean isWestbound = !this.isEastbound;
        final Node originNode = isWestbound ? n2 : n1;
        final Node destinationNode = isWestbound ? n1 : n2;
        final Train shortTrain = new Train("A1", BigDecimal.ONE, BigDecimal.ONE, 90, originNode,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                false, isWestbound);
        Assert.assertTrue("Train shorter than the siding won't be let through.",
                r.isPossibleForTrain(shortTrain));
        final Train longTrain = new Train("A2", BigDecimal.ONE.add(BigDecimal.ONE), BigDecimal.ONE,
                90, originNode, destinationNode, 0, 1, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), false, isWestbound);
        Assert.assertFalse("Train longer than the siding will be let through.",
                r.isPossibleForTrain(longTrain));
    }

    @Test
    public void testIsPossibleForUnrelatedTrain() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a = new Arc(TrackType.MAIN_0, BigDecimal.ONE, n1, n2);
        Route r = new Route(this.isEastbound);
        r = r.extend(a);
        final boolean isWestbound = !this.isEastbound;
        final Node originNode = isWestbound ? n2 : n1;
        final Node destinationNode = isWestbound ? n1 : n2;
        final Train train = new Train("A3", BigDecimal.ONE, BigDecimal.ONE, 90, originNode, n3, 0,
                1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(), false, isWestbound);
        Assert.assertFalse("Train ends in a node not on the route and is let through.",
                r.isPossibleForTrain(train));
        final Train train2 = new Train("A4", BigDecimal.ONE, BigDecimal.ONE, 90, n4,
                destinationNode, 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                false, isWestbound);
        Assert.assertFalse("Train ends in a node not on the route and is let through.",
                r.isPossibleForTrain(train2));
    }
}

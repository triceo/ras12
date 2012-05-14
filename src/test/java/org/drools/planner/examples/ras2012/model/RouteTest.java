package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import junit.framework.Assert;
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
    public void testEquals() {
        Route.resetCounter();
        final Route r = new Route(this.isEastbound);
        final Route r2 = new Route(this.isEastbound);
        Assert.assertEquals("Route should be equal to itself.", r, r);
        Assert.assertFalse("Route should not equal null.", r.equals(null));
        Assert.assertFalse("No two routes should be equal.", r.equals(r2));
        Assert.assertFalse("No two routes should be equal.", r2.equals(r));
        Assert.assertFalse("Route shouldn't equal non-Route.", r.equals("nonsense"));
        // route that's been reset should have the same ID as the original first route
        Route.resetCounter();
        final Route r3 = new Route(this.isEastbound);
        Assert.assertEquals(r, r3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtendNotWithSameArc() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
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
        final Route r2 = r.extend(new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node
                .getNode(1)));
        Assert.assertNotSame("Extended route should be a clone of the original one.", r2, r);
        Assert.assertFalse("Old and extended routes shouldn't be equal.", r2.equals(r));
    }

    @Test
    public void testIsPossibleForHazardousTrain() {
        // prepare route
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Arc a = new Arc(Track.SIDING, BigDecimal.ONE, n1, n2);
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
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
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
        final Arc a = new Arc(Track.SIDING, BigDecimal.ONE, n1, n2);
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
        final Arc a = new Arc(Track.SIDING, BigDecimal.ONE, n1, n2);
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
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
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

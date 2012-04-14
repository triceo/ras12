package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collections;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;
import org.drools.planner.examples.ras2012.model.Route.Direction;
import org.junit.Assert;
import org.junit.Test;

public class ArcTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeLength() {
        new Arc(TrackType.MAIN_0, new BigDecimal("-0.05"), Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull1() {
        new Arc(null, new BigDecimal("10"), Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull2() {
        new Arc(TrackType.MAIN_0, null, Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull3() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), null, Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull4() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), Node.getNode(0), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorSameNodes() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), Node.getNode(0), Node.getNode(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroLength() {
        new Arc(TrackType.MAIN_0, new BigDecimal("0"), Node.getNode(0), Node.getNode(1));
    }

    @Test
    public void testEastWestNodes() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a1 = new Arc(TrackType.MAIN_0, length, n1, n2);
        Assert.assertSame(n1, a1.getWestNode());
        Assert.assertSame(n2, a1.getEastNode());
        final Arc a2 = new Arc(TrackType.MAIN_0, length, n2, n1);
        Assert.assertSame(n1, a2.getEastNode());
        Assert.assertSame(n2, a2.getWestNode());
    }

    @Test
    public void testEqualsObject() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a1 = new Arc(TrackType.MAIN_0, length, n1, n2);
        Assert.assertTrue("The object should equal itself.", a1.equals(a1));
        final Arc a2 = new Arc(TrackType.MAIN_0, length, n1, n2);
        Assert.assertFalse("Objects with the exact same parameters shouldn't equal itself.",
                a1.equals(a2));
    }

    @Test
    public void testStartEndNodesOnRoute() {
        // prepare arc to be tested
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a = new Arc(TrackType.MAIN_0, length, n1, n2);
        // prepare routes, one eastbound and one westbound
        final Route eastbound = new Route(Direction.EASTBOUND).extend(a);
        final Route westbound = new Route(Direction.WESTBOUND).extend(a);
        // and validate their starting and ending nodes
        Assert.assertSame(a.getWestNode(), a.getStartingNode(eastbound));
        Assert.assertSame(a.getEastNode(), a.getEndingNode(eastbound));
        Assert.assertSame(a.getEastNode(), a.getStartingNode(westbound));
        Assert.assertSame(a.getWestNode(), a.getEndingNode(westbound));
    }

    @Test
    public void testStartEndNodesOnTrain() {
        // prepare arc to be tested
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a = new Arc(TrackType.MAIN_0, length, n1, n2);
        // prepare routes, one eastbound and one westbound
        final Train westbound = new Train("A1", BigDecimal.ONE, length, 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train eastbound = new Train("A2", BigDecimal.ONE, length, 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        // and validate their starting and ending nodes
        Assert.assertSame(a.getWestNode(), a.getStartingNode(eastbound));
        Assert.assertSame(a.getEastNode(), a.getEndingNode(eastbound));
        Assert.assertSame(a.getEastNode(), a.getStartingNode(westbound));
        Assert.assertSame(a.getWestNode(), a.getEndingNode(westbound));
    }
}

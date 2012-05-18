package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

public class ArcTest extends AbstractSectionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeLength() {
        new Arc(Track.MAIN_0, new BigDecimal("-0.05"), Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull1() {
        new Arc(null, new BigDecimal("10"), Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull2() {
        new Arc(Track.MAIN_0, null, Node.getNode(0), Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull3() {
        new Arc(Track.MAIN_0, new BigDecimal("10"), null, Node.getNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull4() {
        new Arc(Track.MAIN_0, new BigDecimal("10"), Node.getNode(0), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorSameNodes() {
        new Arc(Track.MAIN_0, new BigDecimal("10"), Node.getNode(0), Node.getNode(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroLength() {
        new Arc(Track.MAIN_0, new BigDecimal("0"), Node.getNode(0), Node.getNode(1));
    }

    @Test
    public void testEastWestNodes() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a1 = new Arc(Track.MAIN_0, length, n1, n2);
        Assert.assertSame(n1, a1.getWestNode());
        Assert.assertSame(n2, a1.getEastNode());
        final Arc a2 = new Arc(Track.MAIN_0, length, n2, n1);
        Assert.assertSame(n1, a2.getEastNode());
        Assert.assertSame(n2, a2.getWestNode());
    }

    @Test
    public void testEqualsObject() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a1 = new Arc(Track.MAIN_0, length, n1, n2);
        Assert.assertTrue("The object should equal itself.", a1.equals(a1));
        final Arc a2 = new Arc(Track.MAIN_0, length, n1, n2);
        Assert.assertFalse("Objects with the exact same parameters shouldn't equal itself.",
                a1.equals(a2));
    }

    @Override
    public void testInitialAndTerminalNodesOnRoute() {
        // prepare arc to be tested
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a = new Arc(Track.MAIN_0, length, n1, n2);
        super.actuallyTestInitialAndTerminalNodesOnRoute(a);
    }

    @Override
    public void testInitialAndTerminalNodesOnTrain() {
        // prepare arc to be tested
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a = new Arc(Track.MAIN_0, length, n1, n2);
        super.actuallyTestInitialAndTerminalNodesOnTrain(a);
    }
}

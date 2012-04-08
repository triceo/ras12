package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;
import org.junit.Assert;
import org.junit.Test;

public class ArcTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeLength() {
        new Arc(TrackType.MAIN_0, new BigDecimal("-0.05"), new Node(0), new Node(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull1() {
        new Arc(null, new BigDecimal("10"), new Node(0), new Node(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull2() {
        new Arc(TrackType.MAIN_0, null, new Node(0), new Node(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull3() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), null, new Node(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull4() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), new Node(0), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorSameNodes() {
        new Arc(TrackType.MAIN_0, new BigDecimal("10"), new Node(0), new Node(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroLength() {
        new Arc(TrackType.MAIN_0, new BigDecimal("0"), new Node(0), new Node(1));
    }

    @Test
    public void testEqualsObject() {
        final Node n1 = new Node(0);
        final Node n2 = new Node(1);
        final BigDecimal length = new BigDecimal("1.5");
        final Arc a1 = new Arc(TrackType.MAIN_0, length, n1, n2);
        Assert.assertTrue("The object should equal itself.", a1.equals(a1));
        final Arc a2 = new Arc(TrackType.MAIN_0, length, n1, n2);
        Assert.assertFalse("Objects with the exact same parameters shouldn't equal itself.",
                a1.equals(a2));
    }
}

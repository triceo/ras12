package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class MaintenanceWindowTest {

    @Test()
    public void testConstructor() {
        final Node EAST = Node.getNode(0);
        final Node WEST = Node.getNode(1);
        final int START = 10;
        final int END = 20;
        final MaintenanceWindow mw = new MaintenanceWindow(WEST, EAST, START, END);
        Assert.assertSame(EAST, mw.getEastNode());
        Assert.assertSame(WEST, mw.getWestNode());
        Assert.assertEquals(START * 60 * 1000, mw.getStart());
        Assert.assertEquals(END * 60 * 1000, mw.getEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeTime1() {
        new MaintenanceWindow(Node.getNode(0), Node.getNode(1), -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeTime2() {
        new MaintenanceWindow(Node.getNode(0), Node.getNode(1), 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull1() {
        new MaintenanceWindow(null, Node.getNode(1), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull2() {
        new MaintenanceWindow(Node.getNode(0), null, 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorSame() {
        new MaintenanceWindow(Node.getNode(1), Node.getNode(1), 0, 1);
    }

    @Test
    public void testIsInside() {
        final int MOW_START = 50;
        final int MOW_END = 51;
        final MaintenanceWindow mow = new MaintenanceWindow(Node.getNode(0), Node.getNode(1),
                MOW_START, MOW_END);
        Assert.assertFalse(mow.isInside(49 * 60 * 1000));
        Assert.assertTrue(mow.isInside(50 * 60 * 1000));
        Assert.assertTrue(mow.isInside(51 * 60 * 1000));
        Assert.assertFalse(mow.isInside(52 * 60 * 1000));
    }
}

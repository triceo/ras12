package org.drools.planner.examples.ras2012.model.original;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class MaintenanceWindowTest extends AbstractSectionTest {

    @Test()
    public void testConstructor() {
        final Node EAST = Node.getNode(0);
        final Node WEST = Node.getNode(1);
        final int START = 10;
        final int END = 20;
        final MaintenanceWindow mw = new MaintenanceWindow(WEST, EAST, START, END);
        Assert.assertSame(EAST, mw.getEastNode());
        Assert.assertSame(WEST, mw.getWestNode());
        Assert.assertEquals(TimeUnit.MINUTES.toMillis(START), mw.getStart(TimeUnit.MILLISECONDS));
        Assert.assertEquals(TimeUnit.MINUTES.toMillis(END), mw.getEnd(TimeUnit.MILLISECONDS));
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

    @Override
    public void testInitialAndTerminalNodesOnRoute() {
        // prepare arc to be tested
        final MaintenanceWindow mow = new MaintenanceWindow(Node.getNode(0), Node.getNode(1), 0, 10);
        super.actuallyTestInitialAndTerminalNodesOnRoute(mow);
    }

    @Override
    public void testInitialAndTerminalNodesOnTrain() {
        // prepare arc to be tested
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final MaintenanceWindow mow = new MaintenanceWindow(n1, n2, 0, 10);
        super.actuallyTestInitialAndTerminalNodesOnRoute(mow);
    }

    @Test
    public void testIsInside() {
        final int MOW_START = 50;
        final int MOW_END = 51;
        final MaintenanceWindow mow = new MaintenanceWindow(Node.getNode(0), Node.getNode(1),
                MOW_START, MOW_END);
        Assert.assertFalse(mow.isInside(MOW_START - 1, TimeUnit.MINUTES));
        Assert.assertTrue(mow.isInside(MOW_START, TimeUnit.MINUTES));
        Assert.assertTrue(mow.isInside(MOW_END, TimeUnit.MINUTES));
        Assert.assertFalse(mow.isInside(MOW_END + 1, TimeUnit.MINUTES));
    }
}

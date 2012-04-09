package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class MaintenanceWindowTest {

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

    @Test
    public void testEqualsObject() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final MaintenanceWindow m1 = new MaintenanceWindow(n1, n2, 0, 2);
        Assert.assertTrue("MaintenanceWindow should equal itself.", m1.equals(m1));
        final MaintenanceWindow m2 = new MaintenanceWindow(n1, n2, 0, 2);
        Assert.assertTrue("MaintenanceWindow should equal another with the same parameters.",
                m1.equals(m2));
        final MaintenanceWindow m3 = new MaintenanceWindow(n1, n3, 0, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m3));
        final MaintenanceWindow m4 = new MaintenanceWindow(n3, n2, 0, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m4));
        final MaintenanceWindow m5 = new MaintenanceWindow(n1, n2, 1, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m5));
        final MaintenanceWindow m6 = new MaintenanceWindow(n1, n2, 0, 1);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m6));
    }
}

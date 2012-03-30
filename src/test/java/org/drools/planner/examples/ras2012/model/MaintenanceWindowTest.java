package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class MaintenanceWindowTest {

    @Test
    public void testEqualsObject() {
        Node n1 = new Node(0);
        Node n2 = new Node(1);
        Node n3 = new Node(2);
        MaintenanceWindow m1 = new MaintenanceWindow(n1, n2, 0, 2);
        Assert.assertTrue("MaintenanceWindow should equal itself.", m1.equals(m1));
        MaintenanceWindow m2 = new MaintenanceWindow(n1, n2, 0, 2);
        Assert.assertTrue("MaintenanceWindow should equal another with the same parameters.",
                m1.equals(m2));
        MaintenanceWindow m3 = new MaintenanceWindow(n1, n3, 0, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m3));
        MaintenanceWindow m4 = new MaintenanceWindow(n3, n2, 0, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m4));
        MaintenanceWindow m5 = new MaintenanceWindow(n1, n2, 1, 2);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m5));
        MaintenanceWindow m6 = new MaintenanceWindow(n1, n2, 0, 1);
        Assert.assertFalse("MaintenanceWindow shouldn't equal another with different parameters.",
                m1.equals(m6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull1() {
        new MaintenanceWindow(null, new Node(1), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNull2() {
        new MaintenanceWindow(new Node(0), null, 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeTime1() {
        new MaintenanceWindow(new Node(0), new Node(1), -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeTime2() {
        new MaintenanceWindow(new Node(0), new Node(1), 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorReverseTime() {
        new MaintenanceWindow(new Node(0), new Node(1), 2, 1);
    }
}

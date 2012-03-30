package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class NodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNode() {
        new Node(-1);
    }

    @Test
    public void testEqualsObject() {
        Node n1 = new Node(0);
        Assert.assertEquals("Node should equal itself.", n1, n1);
        Node n2 = new Node(0);
        Assert.assertEquals("Node should equal other nodes with the same ID.", n1, n2);
        Node n3 = new Node(1);
        Assert.assertFalse("Node shouldn't equal nodes with different IDs.", n1.equals(n3));
    }

}

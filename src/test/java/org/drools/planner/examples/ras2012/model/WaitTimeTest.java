package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class WaitTimeTest {

    @Test
    public void testEqualsObject() {
        final WaitTime wt1 = new WaitTime(1);
        Assert.assertEquals("WaitTime should equal itself.", wt1, wt1);
        final WaitTime wt2 = new WaitTime(1);
        Assert.assertEquals("WaitTime should equal others with the same ID.", wt1, wt2);
        final WaitTime wt3 = new WaitTime(2);
        Assert.assertFalse("WaitTime shouldn't equal others with different IDs.", wt1.equals(wt3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWaitTimeNegative() {
        new WaitTime(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWaitTimeZero() {
        new WaitTime(0);
    }

}

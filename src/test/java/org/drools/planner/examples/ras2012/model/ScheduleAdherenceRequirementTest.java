package org.drools.planner.examples.ras2012.model;

import org.junit.Assert;
import org.junit.Test;

public class ScheduleAdherenceRequirementTest {

    @Test(expected = IllegalArgumentException.class)
    public void testScheduleAdherenceRequirementNegativeTime() {
        new ScheduleAdherenceRequirement(new Node(0), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScheduleAdherenceRequirementNullNode() {
        new ScheduleAdherenceRequirement(null, 1);
    }

    @Test
    public void testEqualsObject() {
        ScheduleAdherenceRequirement s1 = new ScheduleAdherenceRequirement(new Node(0), 1);
        Assert.assertEquals("ScheduleAdherenceRequirement should equal itself.", s1, s1);
        ScheduleAdherenceRequirement s2 = new ScheduleAdherenceRequirement(new Node(0), 1);
        Assert.assertEquals("ScheduleAdherenceRequirement should equal others with the same data.",
                s1, s2);
        ScheduleAdherenceRequirement s3 = new ScheduleAdherenceRequirement(new Node(1), 1);
        Assert.assertFalse(
                "ScheduleAdherenceRequirement shouldn't equal others with different data.",
                s1.equals(s3));
        ScheduleAdherenceRequirement s4 = new ScheduleAdherenceRequirement(new Node(0), 2);
        Assert.assertFalse(
                "ScheduleAdherenceRequirement shouldn't equal others with different data.",
                s1.equals(s4));
    }

}

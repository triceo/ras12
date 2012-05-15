package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Track;
import org.drools.planner.examples.ras2012.util.OccupationTracker.ArcRange;
import org.drools.planner.examples.ras2012.util.OccupationTracker.Builder;
import org.junit.Assert;
import org.junit.Test;

public class OccupationTrackerBuilderTest {

    @Test
    public void testDirectionAdjustment() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        // two full ranges; result should be equal
        final ArcRange r1 = new Builder(new Route(true)).create(a, BigDecimal.ZERO, BigDecimal.ONE);
        final ArcRange r2 = new Builder(new Route(false))
                .create(a, BigDecimal.ZERO, BigDecimal.ONE);
        Assert.assertEquals("Same ranges, opposite directions.", r2, r1);
        Assert.assertEquals("Same ranges, opposite directions.", r1, r2);
        // two equal half ranges; result should be equal
        final ArcRange r3 = new Builder(new Route(true)).create(a, BigDecimal.ZERO, new BigDecimal(
                "0.5"));
        final ArcRange r4 = new Builder(new Route(false)).create(a, new BigDecimal("0.5"),
                BigDecimal.ONE);
        Assert.assertEquals("Same ranges, opposite directions.", r3, r4);
        Assert.assertEquals("Same ranges, opposite directions.", r4, r3);
        // two opposite half ranges; result shouldn't be equal
        final ArcRange r5 = new Builder(new Route(true)).create(a, BigDecimal.ZERO, new BigDecimal(
                "0.5"));
        final ArcRange r6 = new Builder(new Route(false)).create(a, BigDecimal.ZERO,
                new BigDecimal("0.5"));
        Assert.assertFalse("Opposite ranges, opposite directions.", r6.equals(r5));
        Assert.assertFalse("Opposite ranges, opposite directions.", r5.equals(r6));
    }

    @Test
    public void testEmpty() {
        Assert.assertTrue("Empty builder must be empty.", Builder.empty().isEmpty());
        Assert.assertSame("Successive calls to empty() must return the same object.",
                Builder.empty(), Builder.empty());
    }
}

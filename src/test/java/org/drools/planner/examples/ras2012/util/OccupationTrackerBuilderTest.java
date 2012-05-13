package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;

import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Track;
import org.drools.planner.examples.ras2012.util.OccupationTracker.ArcRange;
import org.drools.planner.examples.ras2012.util.OccupationTracker.Builder;
import org.junit.Assert;
import org.junit.Test;

public class OccupationTrackerBuilderTest {

    @Test
    public void testEmpty() {
        Assert.assertTrue("Empty builder must be empty.", Builder.empty().isEmpty());
        Assert.assertSame("Successive calls to empty() must return the same object.",
                Builder.empty(), Builder.empty());
    }

    @Test
    public void testDirectionAdjustment() {
        Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        // two full ranges; result should be equal
        ArcRange r1 = new Builder(new Route(true)).create(a, BigDecimal.ZERO, BigDecimal.ONE);
        ArcRange r2 = new Builder(new Route(false)).create(a, BigDecimal.ZERO, BigDecimal.ONE);
        Assert.assertEquals("Same ranges, opposite directions.", r2, r1);
        // two equal half ranges; result should be equal
        ArcRange r3 = new Builder(new Route(true))
                .create(a, BigDecimal.ZERO, new BigDecimal("0.5"));
        ArcRange r4 = new Builder(new Route(false))
                .create(a, new BigDecimal("0.5"), BigDecimal.ONE);
        Assert.assertEquals("Same ranges, opposite directions.", r4, r3);
        // two opposite half ranges; result should be equal
        ArcRange r5 = new Builder(new Route(true))
                .create(a, BigDecimal.ZERO, new BigDecimal("0.5"));
        ArcRange r6 = new Builder(new Route(false)).create(a, BigDecimal.ZERO,
                new BigDecimal("0.5"));
        Assert.assertFalse("Opposite ranges, opposite directions.", r6.equals(r5));
    }
}

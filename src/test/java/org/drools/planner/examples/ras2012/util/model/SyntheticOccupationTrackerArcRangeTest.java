package org.drools.planner.examples.ras2012.util.model;

import java.math.BigDecimal;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Track;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker.ArcRange;
import org.junit.Assert;
import org.junit.Test;

public class SyntheticOccupationTrackerArcRangeTest {

    private static final Arc ARC  = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0),
                                          Node.getNode(1));
    private static final Arc ARC2 = new Arc(Track.MAIN_1, BigDecimal.ONE, Node.getNode(1),
                                          Node.getNode(2));

    @Test
    public void testComplexConstructor() {
        // full
        ArcRange range = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC, BigDecimal.ZERO,
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
        Assert.assertEquals(SyntheticOccupationTrackerArcRangeTest.ARC, range.getArc());
        Assert.assertTrue(range.isFull());
        Assert.assertFalse(range.isEmpty());
        // empty
        range = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC, BigDecimal.ZERO,
                BigDecimal.ZERO);
        Assert.assertEquals(SyntheticOccupationTrackerArcRangeTest.ARC, range.getArc());
        Assert.assertFalse(range.isFull());
        Assert.assertTrue(range.isEmpty());
        range = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC,
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength(),
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
        Assert.assertEquals(SyntheticOccupationTrackerArcRangeTest.ARC, range.getArc());
        Assert.assertFalse(range.isFull());
        Assert.assertTrue(range.isEmpty());
    }

    @Test
    public void testEqualsObject() {
        final ArcRange range = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC,
                BigDecimal.ZERO, SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
        Assert.assertEquals(range, range);
        Assert.assertFalse(range.equals(null));
        Assert.assertFalse(range.equals("nonsense"));
        Assert.assertFalse("nonsense".equals(range));
        final ArcRange range2 = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC,
                BigDecimal.ONE, SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
        Assert.assertFalse(range.equals(range2));
        Assert.assertFalse(range2.equals(range));
        final ArcRange range3 = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC2,
                BigDecimal.ZERO, SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
        Assert.assertFalse(range3.equals(range));
        Assert.assertFalse(range.equals(range3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeEnd() {
        new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC, BigDecimal.ZERO,
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength().negate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStart() {
        new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC, BigDecimal.ONE.negate(),
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullArc() {
        new ArcRange(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverlongEnd() {
        new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC, BigDecimal.ZERO,
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength().add(BigDecimal.ONE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverlongStart() {
        new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC,
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength().add(BigDecimal.ONE),
                SyntheticOccupationTrackerArcRangeTest.ARC.getLength());
    }

    @Test
    public void testSimpleConstructor() {
        final ArcRange range = new ArcRange(SyntheticOccupationTrackerArcRangeTest.ARC);
        Assert.assertEquals(SyntheticOccupationTrackerArcRangeTest.ARC, range.getArc());
        Assert.assertTrue(range.isFull());
        Assert.assertFalse(range.isEmpty());
    }

}

package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.Assert;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Track;
import org.drools.planner.examples.ras2012.util.OccupationTracker.ArcRange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OccupationTrackerArcRangeTest {

    private static final class TestInfo {

        private final BigDecimal start1, start2, end1, end2, result;
        private final String     description;

        public TestInfo(final int start1, final int start2, final int end1, final int end2,
                final int result, final String description) {
            this.start1 = BigDecimal.valueOf(start1);
            this.start2 = BigDecimal.valueOf(start2);
            this.end1 = BigDecimal.valueOf(end1);
            this.end2 = BigDecimal.valueOf(end2);
            this.result = BigDecimal.valueOf(result);
            this.description = description + " (" + start1 + "," + start2 + "," + end1 + "," + end2
                    + ")";
        }

        public String getDescription() {
            return this.description;
        }

        public BigDecimal getEnd1() {
            return this.end1;
        }

        public BigDecimal getEnd2() {
            return this.end2;
        }

        public BigDecimal getMax() {
            return this.start1.max(this.start2.max(this.end1.max(this.end2)));
        }

        public BigDecimal getResult() {
            return this.result;
        }

        public BigDecimal getStart1() {
            return this.start1;
        }

        public BigDecimal getStart2() {
            return this.start2;
        }

    }

    @Parameters
    public static Collection<Object[]> getInput() {
        final Collection<Object[]> infos = new LinkedList<Object[]>();
        // ranges may overlap; all combinations possible
        infos.add(new Object[] { new TestInfo(1, 0, 2, 3, 1, "start1 > start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 2, 3, 2, "start1 = start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(0, 1, 2, 3, 1, "start1 < start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(1, 0, 2, 2, 1, "start1 > start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 2, 2, 2, "start1 = start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(0, 1, 2, 2, 1, "start1 < start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(1, 0, 3, 2, 1, "start1 > start2,end1 > end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 3, 2, 2, "start1 = start2,end1 > end2") });
        infos.add(new Object[] { new TestInfo(0, 1, 3, 2, 1, "start1 < start2,end1 > end2") });
        // ranges don't overlap; some combinations are impossible
        infos.add(new Object[] { new TestInfo(0, 2, 1, 3, 0, "overlap, start1 < start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(2, 0, 3, 1, 0, "overlap, start1 > start2,end1 > end2") });
        // ranges are equal; only one combination possible
        infos.add(new Object[] { new TestInfo(0, 0, 0, 0, 0, "equal, start1 = start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 1, 1, 1, "equal, start1 = start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 2, 2, 2, "equal, start1 = start2,end1 = end2") });
        // ranges touch; some combinations are impossible
        infos.add(new Object[] { new TestInfo(0, 0, 0, 1, 0, "touch, start1 = start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(0, 1, 1, 2, 0, "touch, start1 < start2,end1 < end2") });
        infos.add(new Object[] { new TestInfo(1, 0, 1, 1, 0, "touch, start1 > start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(1, 2, 2, 2, 0, "touch, start1 < start2,end1 = end2") });
        infos.add(new Object[] { new TestInfo(1, 0, 2, 1, 0, "touch, start1 > start2,end1 > end2") });
        infos.add(new Object[] { new TestInfo(0, 0, 1, 0, 0, "touch, start1 = start2,end1 > end2") });
        return infos;
    }

    private final TestInfo testInfo;

    public OccupationTrackerArcRangeTest(final TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @Test
    public void testRanges() {
        final Arc a = new Arc(Track.MAIN_0, this.testInfo.getMax().max(BigDecimal.ONE),
                Node.getNode(0), Node.getNode(1));
        final ArcRange a1 = new ArcRange(a, this.testInfo.getStart1(), this.testInfo.getEnd1());
        final ArcRange a2 = new ArcRange(a, this.testInfo.getStart2(), this.testInfo.getEnd2());
        Assert.assertEquals(this.testInfo.getDescription(), this.testInfo.getResult(),
                a1.getConflictingMileage(a2));
        Assert.assertEquals(this.testInfo.getDescription(), this.testInfo.getResult(),
                a2.getConflictingMileage(a1));
    }

    @Test
    public void testRangesLonger() {
        final Arc a = new Arc(Track.MAIN_0, this.testInfo.getMax().add(BigDecimal.ONE),
                Node.getNode(0), Node.getNode(1));
        final ArcRange a1 = new ArcRange(a, this.testInfo.getStart1(), this.testInfo.getEnd1());
        final ArcRange a2 = new ArcRange(a, this.testInfo.getStart2(), this.testInfo.getEnd2());
        Assert.assertEquals(this.testInfo.getDescription(), this.testInfo.getResult(),
                a1.getConflictingMileage(a2));
        Assert.assertEquals(this.testInfo.getDescription(), this.testInfo.getResult(),
                a2.getConflictingMileage(a1));
    }

    private void testRangesShorter(final boolean switchRanges) {
        final Arc a = new Arc(Track.MAIN_0, this.testInfo.getMax().subtract(BigDecimal.ONE),
                Node.getNode(0), Node.getNode(1));
        final ArcRange a1 = new ArcRange(a, this.testInfo.getStart1(), this.testInfo.getEnd1());
        final ArcRange a2 = new ArcRange(a, this.testInfo.getStart2(), this.testInfo.getEnd2());
        if (switchRanges) {
            a1.getConflictingMileage(a2);
        } else {
            a2.getConflictingMileage(a1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangesShorterOne() {
        this.testRangesShorter(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangesShorterTwo() {
        this.testRangesShorter(false);
    }
}

package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.Assert;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Track;
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

        public TestInfo(int start1, int start2, int end1, int end2, int result, String description) {
            this.start1 = BigDecimal.valueOf(start1);
            this.start2 = BigDecimal.valueOf(start2);
            this.end1 = BigDecimal.valueOf(end1);
            this.end2 = BigDecimal.valueOf(end2);
            this.result = BigDecimal.valueOf(result);
            this.description = description + " (" + start1 + "," + start2 + "," + end1 + "," + end2
                    + ")";
        }

        public BigDecimal getResult() {
            return result;
        }

        public BigDecimal getStart1() {
            return start1;
        }

        public BigDecimal getStart2() {
            return start2;
        }

        public BigDecimal getEnd1() {
            return end1;
        }

        public BigDecimal getEnd2() {
            return end2;
        }

        public BigDecimal getMax() {
            return start1.max(start2.max(end1.max(end2)));
        }

        public String getDescription() {
            return description;
        }

    }

    @Parameters
    public static Collection<Object[]> getInput() {
        Collection<Object[]> infos = new LinkedList<Object[]>();
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

    public OccupationTrackerArcRangeTest(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @Test
    public void testRanges() {
        Arc a = new Arc(Track.MAIN_0, testInfo.getMax().max(BigDecimal.ONE), Node.getNode(0),
                Node.getNode(1));
        ArcRange a1 = new ArcRange(a, testInfo.getStart1(), testInfo.getEnd1());
        ArcRange a2 = new ArcRange(a, testInfo.getStart2(), testInfo.getEnd2());
        Assert.assertEquals(testInfo.getDescription(), testInfo.getResult(),
                a1.getConflictingMileage(a2));
        Assert.assertEquals(testInfo.getDescription(), testInfo.getResult(),
                a2.getConflictingMileage(a1));
    }

    @Test
    public void testRangesLonger() {
        Arc a = new Arc(Track.MAIN_0, testInfo.getMax().add(BigDecimal.ONE), Node.getNode(0),
                Node.getNode(1));
        ArcRange a1 = new ArcRange(a, testInfo.getStart1(), testInfo.getEnd1());
        ArcRange a2 = new ArcRange(a, testInfo.getStart2(), testInfo.getEnd2());
        Assert.assertEquals(testInfo.getDescription(), testInfo.getResult(),
                a1.getConflictingMileage(a2));
        Assert.assertEquals(testInfo.getDescription(), testInfo.getResult(),
                a2.getConflictingMileage(a1));
    }

    private void testRangesShorter(boolean switchRanges) {
        Arc a = new Arc(Track.MAIN_0, testInfo.getMax().subtract(BigDecimal.ONE), Node.getNode(0),
                Node.getNode(1));
        ArcRange a1 = new ArcRange(a, testInfo.getStart1(), testInfo.getEnd1());
        ArcRange a2 = new ArcRange(a, testInfo.getStart2(), testInfo.getEnd2());
        if (switchRanges) {
            a1.getConflictingMileage(a2);
        } else {
            a2.getConflictingMileage(a1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangesShorterOne() {
        testRangesShorter(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangesShorterTwo() {
        testRangesShorter(false);
    }
}

package org.drools.planner.examples.ras2012.model;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;
import org.junit.Assert;
import org.junit.Test;

public class TrackTypeTest {

    @Test
    public void testMainTracks() {
        for (final TrackType t : TrackType.values()) {
            if (t == TrackType.MAIN_0 || t == TrackType.MAIN_1 || t == TrackType.MAIN_2) {
                Assert.assertTrue(t + " should be a main track.", t.isMainTrack());
            } else {
                Assert.assertFalse(t + " shouldn't be a main track.", t.isMainTrack());
            }
        }
    }

    @Test
    public void testNumberOfValues() {
        Assert.assertEquals(
                "TrackType is only supposed to have values of MAIN_0,MAIN_1,MAIN_2,SIDING,SWITCH and CROSSOVER.",
                6, TrackType.values().length);
    }

    @Test
    public void testSetSpeedOnMainTracks() {
        final int SPEED1 = 90;
        final int SPEED2 = 80;
        for (final TrackType t : TrackType.values()) {
            if (!t.isMainTrack()) {
                continue;
            }
            TrackType.setSpeed(t, SPEED1, SPEED2);
            Assert.assertEquals(SPEED1, t.getSpeedEastbound());
            Assert.assertEquals(SPEED2, t.getSpeedWestbound());
            try {
                TrackType.setSpeed(t, SPEED1, SPEED2);
                Assert.assertEquals(SPEED1, t.getSpeedEastbound());
                Assert.assertEquals(SPEED2, t.getSpeedWestbound());
            } catch (final IllegalStateException ex) {
                Assert.fail("Re-setting speeds to the values they already have shouldn't fail!");
            }
            try {
                TrackType.setSpeed(t, SPEED2);
                Assert.fail("Setting already assigned speeds should fail!");
            } catch (final IllegalStateException ex) {
                Assert.assertEquals(SPEED1, t.getSpeedEastbound());
                Assert.assertEquals(SPEED2, t.getSpeedWestbound());
            }
            try {
                TrackType.setSpeed(t, SPEED1);
                Assert.fail("Setting already assigned speeds should fail!");
            } catch (final IllegalStateException ex) {
                Assert.assertEquals(SPEED1, t.getSpeedEastbound());
                Assert.assertEquals(SPEED2, t.getSpeedWestbound());
            }
        }
    }

    @Test
    public void testSetSpeedOutsideMainTracks() {
        final int SPEED1 = 90;
        final int SPEED2 = 80;
        for (final TrackType t : TrackType.values()) {
            if (t.isMainTrack()) {
                continue;
            }
            try { // test differing speeds, which shouldn't be allowed
                TrackType.setSpeed(t, SPEED1, SPEED2);
                Assert.fail("Outside main tracks, eastbound and westbound speeds mustn't differ!");
            } catch (final IllegalArgumentException ex) {
                // this is Ok
            }
            TrackType.setSpeed(t, SPEED1); // valid setSpeed()
            Assert.assertSame(t.getSpeedEastbound(), t.getSpeedWestbound());
            Assert.assertEquals(SPEED1, t.getSpeedEastbound());
            try {
                TrackType.setSpeed(t, SPEED1, SPEED1);
                TrackType.setSpeed(t, SPEED1);
                Assert.assertSame(t.getSpeedEastbound(), t.getSpeedWestbound());
                Assert.assertEquals(SPEED1, t.getSpeedEastbound());
            } catch (final IllegalStateException ex) {
                Assert.fail("Re-setting speeds to the values they already have shouldn't fail!");
            }
            try {
                TrackType.setSpeed(t, SPEED2);
                Assert.fail("Setting already assigned speeds should fail!");
            } catch (final IllegalStateException ex) {
                Assert.assertSame(t.getSpeedEastbound(), t.getSpeedWestbound());
                Assert.assertEquals(SPEED1, t.getSpeedEastbound());
            }
        }
    }
}

package org.drools.planner.examples.ras2012.model;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;
import org.junit.Assert;
import org.junit.Ignore;
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
    @Ignore
    public void testSpeedImmutability() {
        // FIXME write me!
    }

}

package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;

// TODO rework so that speeds aren't static
public enum Track {

    MAIN_0, MAIN_1, MAIN_2, SWITCH(false), SIDING(false), CROSSOVER(false);

    private final boolean                    isMainTrack;

    private static final Map<Track, Integer> speedsWestbound = new HashMap<Track, Integer>();

    private static final Map<Track, Integer> speedsEastbound = new HashMap<Track, Integer>();

    public static void setSpeed(final Track t, final int speed) {
        Track.setSpeed(t, speed, speed);
    }

    public static void setSpeed(final Track t, final int speedEastbound, final int speedWestbound) {
        if (!t.isMainTrack() && speedEastbound != speedWestbound) {
            throw new IllegalArgumentException(
                    "Speeds only differ based on direction when we're on a main track!");
        }
        Track.setSpeedEastbound(t, speedEastbound);
        Track.setSpeedWestbound(t, speedWestbound);
    }

    private static void setSpeedEastbound(final Track t, final int speed) {
        if (Track.speedsEastbound.get(t) != null && Track.speedsEastbound.get(t) != speed) {
            throw new IllegalStateException(
                    "Cannot re-assign an already assigned eastbound track speed.");
        }
        Track.speedsEastbound.put(t, speed);
    }

    private static void setSpeedWestbound(final Track t, final int speed) {
        if (Track.speedsWestbound.get(t) != null && Track.speedsWestbound.get(t) != speed) {
            throw new IllegalStateException(
                    "Cannot re-assign an already assigned westbound track speed.");
        }
        Track.speedsWestbound.put(t, speed);
    }

    Track() {
        this(true);
    }

    Track(final boolean isMain) {
        this.isMainTrack = isMain;
    }

    public int getSpeedEastbound() {
        return Track.speedsEastbound.get(this);
    }

    public int getSpeedWestbound() {
        return Track.speedsWestbound.get(this);
    }

    public boolean isMainTrack() {
        return this.isMainTrack;
    }
}
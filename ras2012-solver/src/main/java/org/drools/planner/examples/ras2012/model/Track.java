package org.drools.planner.examples.ras2012.model;

import java.util.HashMap;
import java.util.Map;

// TODO rework so that speeds aren't static
/**
 * A track is a type of railroad that the arc consists of. It may have different maximum speeds based on the {@link Train}
 * direction. The speeds need to be assigned before the instances can be used.
 * 
 */
public enum Track {

    MAIN_0('='), MAIN_1('<'), MAIN_2('>'), SWITCH('/', false), SIDING('S', false), CROSSOVER('C',
            false);

    private final boolean                    isMainTrack;

    private static final Map<Track, Integer> speedsWestbound = new HashMap<>();

    private static final Map<Track, Integer> speedsEastbound = new HashMap<>();

    /**
     * Set the maximum speed on a particular track, both directions. Speed can only be set once.
     * 
     * @param t The track in question.
     * @param speed The speed.
     */
    public static void setSpeed(final Track t, final int speed) {
        Track.setSpeed(t, speed, speed);
    }

    /**
     * Set the maximum speed on a particular track, each direction separately. Speed can only be set once.
     * 
     * @param t Te track in question.
     * @param speedEastbound Speed when travelling to the east.
     * @param speedWestbound Speed when travelling to the west.
     */
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

    private final char symbol;

    Track(final char symbol) {
        this(symbol, true);
    }

    Track(final char symbol, final boolean isMain) {
        this.symbol = symbol;
        this.isMainTrack = isMain;
    }

    public int getSpeedEastbound() {
        return Track.speedsEastbound.get(this);
    }

    public int getSpeedWestbound() {
        return Track.speedsWestbound.get(this);
    }

    public char getSymbol() {
        return this.symbol;
    }

    public boolean isMainTrack() {
        return this.isMainTrack;
    }
}
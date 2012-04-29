package org.drools.planner.examples.ras2012.model.original;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Arc extends Section {

    // TODO rework so that speeds aren't static
    public enum TrackType {

        MAIN_0, MAIN_1, MAIN_2, SWITCH(false), SIDING(false), CROSSOVER(false);

        private final boolean                        isMainTrack;

        private static final Map<TrackType, Integer> speedsWestbound = new HashMap<TrackType, Integer>();

        private static final Map<TrackType, Integer> speedsEastbound = new HashMap<TrackType, Integer>();

        public static void setSpeed(final TrackType t, final int speed) {
            TrackType.setSpeed(t, speed, speed);
        }

        public static void setSpeed(final TrackType t, final int speedEastbound,
                final int speedWestbound) {
            if (!t.isMainTrack() && speedEastbound != speedWestbound) {
                throw new IllegalArgumentException(
                        "Speeds only differ based on direction when we're on a main track!");
            }
            TrackType.setSpeedEastbound(t, speedEastbound);
            TrackType.setSpeedWestbound(t, speedWestbound);
        }

        private static void setSpeedEastbound(final TrackType t, final int speed) {
            if (TrackType.speedsEastbound.get(t) != null
                    && TrackType.speedsEastbound.get(t) != speed) {
                throw new IllegalStateException(
                        "Cannot re-assign an already assigned eastbound track speed.");
            }
            TrackType.speedsEastbound.put(t, speed);
        }

        private static void setSpeedWestbound(final TrackType t, final int speed) {
            if (TrackType.speedsWestbound.get(t) != null
                    && TrackType.speedsWestbound.get(t) != speed) {
                throw new IllegalStateException(
                        "Cannot re-assign an already assigned westbound track speed.");
            }
            TrackType.speedsWestbound.put(t, speed);
        }

        TrackType() {
            this(true);
        }

        TrackType(final boolean isMain) {
            this.isMainTrack = isMain;
        }

        public int getSpeedEastbound() {
            return TrackType.speedsEastbound.get(this);
        }

        public int getSpeedWestbound() {
            return TrackType.speedsWestbound.get(this);
        }

        public boolean isMainTrack() {
            return this.isMainTrack;
        }
    }

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final int                  id          = Arc.idGenerator.incrementAndGet();

    private final TrackType            trackType;

    private final BigDecimal           lengthInMiles;

    private final String               asString;

    public Arc(final TrackType t, final BigDecimal lengthInMiles, final Node westNode,
            final Node eastNode) {
        super(westNode, eastNode);
        if (t == null || lengthInMiles == null) {
            throw new IllegalArgumentException("Neither of the arguments can be null.");
        }
        if (BigDecimal.ZERO.compareTo(lengthInMiles) > -1) {
            throw new IllegalArgumentException("Arc length must be greater than zero.");
        }
        this.trackType = t;
        this.lengthInMiles = lengthInMiles;
        this.asString = this.toStringInternal();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Arc)) {
            return false;
        }
        final Arc other = (Arc) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public BigDecimal getLengthInMiles() {
        return this.lengthInMiles;
    }

    public TrackType getTrackType() {
        return this.trackType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        return result;
    }

    @Override
    public String toString() {
        return this.asString;
    }

    private String toStringInternal() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Arc [id=").append(this.id).append(", trackType=").append(this.trackType)
                .append(", lengthInMiles=").append(this.lengthInMiles).append(", section=")
                .append(super.toString()).append("]");
        return builder.toString();
    }

}

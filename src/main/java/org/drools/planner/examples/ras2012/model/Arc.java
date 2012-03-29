package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Arc {

    public enum TrackType {

        MAIN_0, MAIN_1, MAIN_2, SWITCH(false), SIDING(false), CROSSOVER(false);

        private final boolean                        isMainTrack;

        private static final Map<TrackType, Integer> speedsWestbound = new HashMap<TrackType, Integer>();

        private static final Map<TrackType, Integer> speedsEastbound = new HashMap<TrackType, Integer>();

        public static void setSpeed(final TrackType t, final int speed) {
            TrackType.setSpeedEastbound(t, speed);
            TrackType.setSpeedWestbound(t, speed);
        }

        public static void setSpeed(final TrackType t, final int speedEastbound,
                final int speedWestbound) {
            if (!t.isMainTrack()) {
                throw new IllegalArgumentException(
                        "Speeds only differ based on direction when we're on a main track!");
            }
            TrackType.setSpeedEastbound(t, speedEastbound);
            TrackType.setSpeedWestbound(t, speedWestbound);
        }

        private static void setSpeedEastbound(final TrackType t, final int speed) {
            if (TrackType.speedsEastbound.get(t) != null) {
                throw new IllegalStateException(
                        "Cannot re-assign an already assigned eastbound track speed.");
            }
            TrackType.speedsEastbound.put(t, speed);
        }

        private static void setSpeedWestbound(final TrackType t, final int speed) {
            if (TrackType.speedsWestbound.get(t) != null) {
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

    private final Integer              id          = Arc.idGenerator.incrementAndGet();

    private final TrackType            trackType;

    private final BigDecimal           lengthInMiles;

    private final Node                 startingNode;
    private final Node                 endingNode;

    public Arc(final TrackType t, final BigDecimal lengthInMiles, final Node startingNode,
            final Node endingNode) {
        this.trackType = t;
        this.lengthInMiles = lengthInMiles;
        this.startingNode = startingNode;
        this.endingNode = endingNode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Arc other = (Arc) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public Node getEndingNode() {
        return this.endingNode;
    }

    public BigDecimal getLengthInMiles() {
        return this.lengthInMiles;
    }

    public Node getStartingNode() {
        return this.startingNode;
    }

    public TrackType getTrackType() {
        return this.trackType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.id == null ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Arc [").append(this.startingNode).append("->").append(this.endingNode)
                .append(", miles=").append(this.lengthInMiles).append(", type=")
                .append(this.trackType).append("]");
        return builder.toString();
    }

}

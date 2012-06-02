package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.util.Converter;

public class Train implements Comparable<Train>, Directed {

    public static enum Type {
        A(600), B(500), C(400), D(300), E(150, false), F(100, false);

        private final boolean adhereToSchedule;
        private final int     delayPenalty;

        Type(final int delayPenalty) {
            this(delayPenalty, true);
        }

        Type(final int delayPenalty, final boolean adhereToSchedule) {
            this.adhereToSchedule = adhereToSchedule;
            this.delayPenalty = delayPenalty;
        }

        public boolean adhereToSchedule() {
            return this.adhereToSchedule;
        }

        public int getDelayPenalty() {
            return this.delayPenalty;
        }
    }

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private static Type determineType(final String name) {
        final char[] chars = name.toCharArray();
        switch (chars[0]) {
            case 'A':
                return Type.A;
            case 'B':
                return Type.B;
            case 'C':
                return Type.C;
            case 'D':
                return Type.D;
            case 'E':
                return Type.E;
            case 'F':
                return Type.F;
            default:
                throw new IllegalArgumentException("Invalid train type: " + chars[0]);
        }
    }

    private final String                                  name;

    private final BigDecimal                              length;
    private final BigDecimal                              speedMultiplier;
    private final Type                                    type;
    private final int                                     tob;
    private final Node                                    origin;
    private final Node                                    destination;
    private final long                                    entryTime;
    private final long                                    wantTime;
    private final long                                    originalDelay;
    private final Map<Node, ScheduleAdherenceRequirement> scheduleAdherenceRequirements = new HashMap<Node, ScheduleAdherenceRequirement>();
    private final boolean                                 carriesHazardousMaterials;

    private final boolean                                 isWestbound;

    public Train(final String name, final BigDecimal length, final BigDecimal speedMultiplier,
            final int tob, final Node origin, final Node destination, final int entryTime,
            final int wantTime, final int originalScheduleAdherence,
            final List<ScheduleAdherenceRequirement> sars, final boolean hazmat,
            final boolean isWestbound) {
        if (name == null) {
            throw new IllegalArgumentException("Train name must be a non-empty String.");
        }
        this.type = Train.determineType(name);
        try {
            if (Integer.valueOf(name.substring(1)) < 0) {
                throw new NumberFormatException("Negative numbers not allowed!");
            }
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Train names must be in format of [A-F][0-9]+: "
                    + name);
        }
        this.name = name;
        if (length == null || length.signum() <= 0) {
            throw new IllegalArgumentException("Train must have a length greater than 0.");
        }
        this.length = length;
        if (speedMultiplier == null || speedMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Train must have a speed multiplier greater than 0.");
        }
        this.speedMultiplier = speedMultiplier;
        if (tob < 1) {
            throw new IllegalArgumentException("Train must have TOB > 0.");
        }
        this.tob = tob;
        if (origin == null || destination == null || origin.equals(destination)) {
            throw new IllegalArgumentException(
                    "Train origin and destination must be two different valid nodes.");
        }
        this.origin = origin;
        this.destination = destination;
        if (entryTime < 0) {
            throw new IllegalArgumentException("Train entry time may not be negative.");
        }
        this.entryTime = Train.DEFAULT_TIME_UNIT.convert(entryTime, TimeUnit.MINUTES);
        if (wantTime < 0) {
            throw new IllegalArgumentException("Train want time may not be negative.");
        }
        this.wantTime = Train.DEFAULT_TIME_UNIT.convert(wantTime, TimeUnit.MINUTES);
        this.originalDelay = Train.DEFAULT_TIME_UNIT.convert(originalScheduleAdherence,
                TimeUnit.MINUTES);

        this.carriesHazardousMaterials = hazmat;
        this.isWestbound = isWestbound;
        if (sars != null) {
            for (final ScheduleAdherenceRequirement sa : sars) {
                this.scheduleAdherenceRequirements.put(sa.getDestination(), sa);
            }
        }
    }

    public boolean carriesHazardousMaterials() {
        return this.carriesHazardousMaterials;
    }

    /**
     * Trains are compared by their first letter as strings, then by their number as integers. For example, A2 < B1 and A2 <
     * A11.
     * 
     * @return String.compareTo(String)
     */
    @Override
    public int compareTo(final Train arg0) {
        return new CompareToBuilder()
                .append(this.getName().charAt(0), arg0.getName().charAt(0))
                .append(Integer.valueOf(this.getName().substring(1)),
                        Integer.valueOf(arg0.getName().substring(1))).toComparison();
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
        final Train other = (Train) obj;
        return new EqualsBuilder().append(this.getName(), other.getName()).isEquals();
    }

    public long getArcTravellingTime(final Arc a, final TimeUnit unit) {
        if (a == null) {
            throw new IllegalArgumentException("Arc cannot be null!");
        }
        return unit.convert(
                Converter.getTimeFromSpeedAndDistance(this.getMaximumSpeed(a.getTrack()),
                        a.getLength()), TimeUnit.MILLISECONDS);
    }

    public Node getDestination() {
        return this.destination;
    }

    public long getEntryTime(final TimeUnit unit) {
        return unit.convert(this.entryTime, Train.DEFAULT_TIME_UNIT);
    }

    public BigDecimal getLength() {
        return this.length;
    }

    public BigDecimal getMaximumSpeed() {
        return this.getMaximumSpeed(Track.MAIN_0);
    }

    public BigDecimal getMaximumSpeed(final Track t) {
        final int coreSpeed = this.isWestbound() ? t.getSpeedWestbound() : t.getSpeedEastbound();
        if (t.isMainTrack()) {
            return this.speedMultiplier.multiply(BigDecimal.valueOf(coreSpeed)).setScale(1,
                    BigDecimal.ROUND_HALF_EVEN);
        } else {
            return BigDecimal.valueOf(coreSpeed);
        }
    }

    public String getName() {
        return this.name;
    }

    public Node getOrigin() {
        return this.origin;
    }

    // negative means train is running late
    public long getOriginalSA(final TimeUnit unit) {
        return unit.convert(this.originalDelay, Train.DEFAULT_TIME_UNIT);
    }

    public Map<Node, ScheduleAdherenceRequirement> getScheduleAdherenceRequirements() {
        return this.scheduleAdherenceRequirements;
    }

    protected BigDecimal getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public Type getType() {
        return this.type;
    }

    public long getWantTime(final TimeUnit unit) {
        return unit.convert(this.wantTime, Train.DEFAULT_TIME_UNIT);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getName()).build();
    }

    @Override
    public boolean isEastbound() {
        return !this.isWestbound();
    }

    public boolean isHeavy() {
        return this.tob > 100;
    }

    @Override
    public boolean isWestbound() {
        return this.isWestbound;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Train [name=").append(this.name).append(", length=").append(this.length)
                .append(", origin=").append(this.origin).append(", destination=")
                .append(this.destination).append(", entryTime=").append(this.entryTime)
                .append(", wantTime=").append(this.wantTime).append(", originalDelay=")
                .append(this.originalDelay).append(", scheduleAdherenceRequirements=")
                .append(this.scheduleAdherenceRequirements).append(", carriesHazardousMaterials=")
                .append(this.carriesHazardousMaterials).append(", isWestbound=")
                .append(this.isWestbound).append(", getType()=").append(this.getType())
                .append(", isHeavy()=").append(this.isHeavy()).append("]");
        return builder.toString();
    }

}

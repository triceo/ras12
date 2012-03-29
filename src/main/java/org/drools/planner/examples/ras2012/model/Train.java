package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;

public class Train {

    public static enum TrainType {
        A, B, C, D, E(false), F(false);

        private final boolean adhereToSchedule;

        TrainType() {
            this(true);
        }

        TrainType(final boolean adhereToSchedule) {
            this.adhereToSchedule = adhereToSchedule;
        }

        // FIXME would be cool if this was rule-based
        public boolean adhereToSchedule() {
            return this.adhereToSchedule;
        }
    }

    private static final AtomicInteger               idGenerator = new AtomicInteger();

    private final Integer                            id          = Train.idGenerator
                                                                         .incrementAndGet();

    private final String                             name;

    private final BigDecimal                         length;

    private final BigDecimal                         speedMultiplier;

    private final int                                tob;
    private final Node                               origin;
    private final Node                               destination;
    private final int                                entryTime;
    private final int                                wantTime;
    private final int                                originalDelay;
    private final List<ScheduleAdherenceRequirement> scheduleAdherenceRequirements;
    private final boolean                            carriesHazardousMaterials;
    private final boolean                            isWestbound;

    public Train(final String name, final BigDecimal length, final BigDecimal speedMultiplier,
            final int tob, final Node origin, final Node destination, final int entryTime,
            final int wantTime, final int originalScheduleAdherence,
            final List<ScheduleAdherenceRequirement> sars, final boolean hazmat,
            final boolean isWestbound) {
        this.name = name;
        this.length = length;
        this.speedMultiplier = speedMultiplier;
        this.tob = tob;
        this.origin = origin;
        this.destination = destination;
        this.entryTime = entryTime;
        this.wantTime = wantTime;
        this.originalDelay = originalScheduleAdherence;
        this.scheduleAdherenceRequirements = Collections.unmodifiableList(sars);
        this.carriesHazardousMaterials = hazmat;
        this.isWestbound = isWestbound;
    }

    public boolean carriesHazardousMaterials() {
        return this.carriesHazardousMaterials;
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
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public Node getDestination() {
        return this.destination;
    }

    public int getEntryTime() {
        return this.entryTime;
    }

    public BigDecimal getLength() {
        return this.length;
    }

    // FIXME would be cool if this was rule-based
    public Integer getMaximumSpeed(final TrackType t) {
        final int coreSpeed = this.isWestbound() ? t.getSpeedWestbound() : t.getSpeedEastbound();
        if (t.isMainTrack()) {
            return this.speedMultiplier.multiply(new BigDecimal(coreSpeed)).intValue();
        } else {
            return coreSpeed;
        }
    }

    public String getName() {
        return this.name;
    }

    public Node getOrigin() {
        return this.origin;
    }

    public int getOriginalDelay() {
        return this.originalDelay;
    }

    public List<ScheduleAdherenceRequirement> getScheduleAdherenceRequirements() {
        return this.scheduleAdherenceRequirements;
    }

    // FIXME cache this
    public TrainType getType() {
        final String firstCharOfName = this.name.substring(0, 1);
        switch (firstCharOfName) {
            case "A":
                return TrainType.A;
            case "B":
                return TrainType.B;
            case "C":
                return TrainType.C;
            case "D":
                return TrainType.D;
            case "E":
                return TrainType.E;
            case "F":
                return TrainType.F;
            default:
                throw new IllegalArgumentException("Invalid train type: " + firstCharOfName);
        }
    }

    public int getWantTime() {
        return this.wantTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.id == null ? 0 : this.id.hashCode());
        return result;
    }

    public boolean isEastbound() {
        return !this.isWestbound();
    }

    public boolean isHeavy() {
        return this.tob > 100;
    }

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

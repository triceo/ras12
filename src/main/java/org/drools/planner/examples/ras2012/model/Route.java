package org.drools.planner.examples.ras2012.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.planner.examples.ras2012.model.Arc.TrackType;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Route implements Comparable<Route> {

    public static enum Direction {
        EASTBOUND, WESTBOUND
    }

    protected static int resetRouteCounter() {
        return Route.idGenerator.getAndSet(0);
    }

    private final List<Arc>            parts       = new LinkedList<Arc>();
    private final Direction            direction;

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int                  id          = Route.idGenerator.getAndIncrement();

    public Route(final Direction d) {
        this.direction = d;
    }

    private Route(final Direction d, final Arc... e) {
        this(d);
        for (final Arc a : e) {
            this.parts.add(a);
        }
    }

    // FIXME add tests for this
    @Override
    public int compareTo(final Route o) {
        if (this.direction == o.direction) { // shorter = better
            final int comparison = this.getTravellingTimeInMinutes().compareTo(
                    o.getTravellingTimeInMinutes());
            if (comparison == 0) { // more main tracks = better
                return this.getNumberOfMainTracks() - o.getNumberOfMainTracks();
            } else {
                return comparison;
            }
        } else {
            if (this.direction == Direction.EASTBOUND) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public boolean contains(final Arc e) {
        return this.parts.contains(e);
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
        final Route other = (Route) obj;
        if (this.direction != other.direction) {
            return false;
        }
        if (this.parts == null) {
            if (other.parts != null) {
                return false;
            }
        } else if (!this.parts.equals(other.parts)) {
            return false;
        }
        return true;
    }

    public Route extend(final Arc add) {
        if (add == null) {
            throw new IllegalArgumentException("Cannot extend route with a null arc!");
        }
        if (this.parts.contains(add)) {
            throw new IllegalArgumentException("Cannot extend route with the same arc twice!");
        }
        final Collection<Arc> newParts = new ArrayList<Arc>(this.parts);
        newParts.add(add);
        final Arc[] result = newParts.toArray(new Arc[newParts.size()]);
        return new Route(this.getDirection(), result);
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getId() {
        return this.id;
    }

    public Arc getInitialArc() {
        if (this.direction == Direction.WESTBOUND) {
            return this.parts.get(this.parts.size() - 1);
        } else {
            return this.parts.get(0);
        }
    }

    public int getLengthInArcs() {
        return this.parts.size();
    }

    public BigDecimal getLengthInMiles() {
        BigDecimal result = BigDecimal.ZERO;
        for (final Arc a : this.parts) {
            result = result.add(a.getLengthInMiles());
        }
        return result;
    }

    // FIXME add tests for this
    public int getLengthInNodes() {
        return this.parts.size();
    }

    public Arc getNextArc(final Arc a) {
        if (this.parts.isEmpty()) {
            throw new IllegalArgumentException("There is no next arc in an empty route.");
        }
        if (a == null) {
            return this.getInitialArc();
        }
        if (!this.contains(a)) {
            throw new IllegalArgumentException("The route doesn't contain the arc.");
        }
        final int index = this.parts.indexOf(a);
        if (this.direction == Direction.WESTBOUND) {
            if (index == 0) {
                return null;
            } else {
                return this.parts.get(index - 1);
            }
        } else {
            if (index == this.parts.size() - 1) {
                return null;
            } else {
                return this.parts.get(index + 1);
            }
        }
    }

    // FIXME add tests for this
    public int getNumberOfMainTracks() {
        int i = 0;
        for (final Arc a : this.parts) {
            if (a.getTrackType().isMainTrack()) {
                i++;
            }
        }
        return i;
    }

    public Arc getTerminalArc() {
        if (this.direction == Direction.EASTBOUND) {
            return this.parts.get(this.parts.size() - 1);
        } else {
            return this.parts.get(0);
        }
    }

    // FIXME add tests for this
    public BigDecimal getTravellingTimeInMinutes() {
        BigDecimal result = BigDecimal.ZERO;
        for (final Arc a : this.parts) {
            final BigDecimal length = a.getLengthInMiles();
            final int speed = this.direction == Direction.EASTBOUND ? a.getTrackType()
                    .getSpeedEastbound() : a.getTrackType().getSpeedWestbound();
            final BigDecimal timeInHours = length.divide(BigDecimal.valueOf(speed), 2,
                    BigDecimal.ROUND_HALF_DOWN);
            result = result.add(timeInHours.multiply(BigDecimal.valueOf(60)));
        }
        return result;
    }

    public Collection<Node> getWaitPoints() {
        final Collection<Node> waitPoints = new HashSet<Node>();
        // we want to be able to hold the train before it enters the network
        final Arc firstArc = this.getInitialArc();
        if (this.direction == Direction.EASTBOUND) {
            waitPoints.add(firstArc.getWestNode());
        } else {
            waitPoints.add(firstArc.getEastNode());
        }
        // other wait points depend on the type of the track
        for (final Arc a : this.parts) {
            if (a.getTrackType() == TrackType.SIDING) {
                // on sidings, wait before leaving them through a switch
                if (this.direction == Direction.EASTBOUND) {
                    waitPoints.add(a.getEastNode());
                } else {
                    waitPoints.add(a.getWestNode());
                }
            } else if (!a.getTrackType().isMainTrack()) {
                // on crossovers and switches, wait before joining them
                if (this.direction == Direction.EASTBOUND) {
                    waitPoints.add(a.getWestNode());
                } else {
                    waitPoints.add(a.getEastNode());
                }
            } else {
                // on main tracks, never wait
            }
        }
        return Collections.unmodifiableCollection(waitPoints);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.direction == null ? 0 : this.direction.hashCode());
        result = prime * result + (this.parts == null ? 0 : this.parts.hashCode());
        return result;
    }

    public boolean isPossibleForTrain(final Train t) {
        // make sure both the route and the train are in the same direction
        final boolean bothEastbound = t.isEastbound() && this.getDirection() == Direction.EASTBOUND;
        final boolean bothWestbound = t.isWestbound() && this.getDirection() == Direction.WESTBOUND;
        if (!bothEastbound && !bothWestbound) {
            return false;
        }
        // make sure the route doesn't contain a siding shorter than the train
        for (final Arc a : this.parts) {
            if (a.getTrackType() == TrackType.SIDING) {
                final int result = a.getLengthInMiles().compareTo(t.getLength());
                if (result < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean toCSV(final OutputStream os) {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os))) {
            if (this.getDirection() == Direction.EASTBOUND) {
                w.write("west;east;length;speed");
            } else {
                w.write("east;west;length;speed");
            }
            w.newLine();
            for (final Arc arc : this.parts) {
                if (this.getDirection() == Direction.EASTBOUND) {
                    w.write(String.valueOf(arc.getWestNode().getId()));
                    w.write(";");
                    w.write(String.valueOf(arc.getEastNode().getId()));
                    w.write(";");
                } else {
                    w.write(String.valueOf(arc.getEastNode().getId()));
                    w.write(";");
                    w.write(String.valueOf(arc.getWestNode().getId()));
                    w.write(";");
                }
                w.write(arc.getLengthInMiles().toString());
                w.write(";");
                if (this.getDirection() == Direction.EASTBOUND) {
                    w.write(String.valueOf(arc.getTrackType().getSpeedEastbound()));
                } else {
                    w.write(String.valueOf(arc.getTrackType().getSpeedWestbound()));
                }
                w.newLine();
            }
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Route [id=" + this.id + ", direction=" + this.direction + ", parts=" + this.parts
                + "]";
    }
}

package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;

/**
 * The arc represents a single length of railroad track, whose properties such as maximum speed are uniform all over it.
 * {@link Route} consist solely of arcs, each arc has the west and the east {@link Node}.
 * 
 */
public class Arc extends Section {

    private final Track      track;

    private final BigDecimal lengthInMiles;

    /**
     * Creates a new instance.
     * 
     * @param t Type of track that this arc is.
     * @param lengthInMiles Length of the arc in miles.
     * @param westNode The west-most end of the arc.
     * @param eastNode The east-most end of the arc.
     */
    public Arc(final Track t, final BigDecimal lengthInMiles, final Node westNode,
            final Node eastNode) {
        super(westNode, eastNode);
        if (t == null || lengthInMiles == null) {
            throw new IllegalArgumentException("Neither of the arguments can be null.");
        }
        if (lengthInMiles.signum() <= 0) {
            throw new IllegalArgumentException("Arc length must be greater than zero.");
        }
        this.track = t;
        this.lengthInMiles = lengthInMiles;
    }

    public BigDecimal getLength() {
        return this.lengthInMiles;
    }

    public Track getTrack() {
        return this.track;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Arc (").append(this.track.getSymbol()).append(") [lengthInMiles=")
                .append(this.lengthInMiles).append(", section=").append(super.toString())
                .append("]");
        return builder.toString();
    }
}

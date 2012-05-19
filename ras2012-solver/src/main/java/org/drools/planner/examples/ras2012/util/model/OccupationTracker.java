package org.drools.planner.examples.ras2012.util.model;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.model.Arc;

public class OccupationTracker {

    protected static final class ArcRange {

        private final Arc arc;

        private final boolean full, empty;

        private final BigDecimal start, end;

        public ArcRange(final Arc a) {
            this(a, BigDecimal.ZERO);
        }

        public ArcRange(final Arc a, final BigDecimal start) {
            this(a, start, null);
        }

        public ArcRange(final Arc a, final BigDecimal start, final BigDecimal end) {
            if (a == null) {
                throw new IllegalArgumentException("Arc must not be null.");
            }
            arc = a;
            if (start.signum() < 0) {
                throw new IllegalArgumentException("Arc range start must be in the range of <0,"
                        + a.getLength() + ">.");
            }
            this.start = start;
            if (end == null) {
                this.end = a.getLength();
            } else {
                if (end.compareTo(this.start) < 0 || end.compareTo(a.getLength()) > 0) {
                    throw new IllegalArgumentException("Arc range end must be in the range of <"
                            + this.start + "," + a.getLength() + ">.");
                }
                this.end = end;
            }
            full = getStart().equals(BigDecimal.ZERO)
                    && getEnd().equals(a.getLength());
            empty = getStart().equals(getEnd());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ArcRange)) {
                return false;
            }
            final ArcRange other = (ArcRange) obj;
            if (arc != other.arc) {
                return false;
            }
            if (isFull() && other.isFull() || isEmpty() && other.isEmpty()) {
                return true;
            }
            if (!end.equals(other.end)) {
                return false;
            }
            if (!start.equals(other.start)) {
                return false;
            }
            return true;
        }

        public Arc getArc() {
            return arc;
        }

        public BigDecimal getConflictingMileage(final ArcRange other) {
            if (getArc() != other.getArc()) {
                // ranges cannot conflict when they're of two different arcs
                return BigDecimal.ZERO;
            } else if (isFull() && other.isEmpty()) {
                // the intersection is empty
                return BigDecimal.ZERO;
            } else if (other.isFull() && isEmpty()) {
                // the intersection is empty
                return BigDecimal.ZERO;
            } else if (isFull() && other.isFull()) {
                // full conflict
                return getArc().getLength();
            } else {
                // we need to calculate the actual conflicting range
                final BigDecimal start = getStart().max(other.getStart());
                final BigDecimal end = getEnd().min(other.getEnd());
                final BigDecimal result = end.subtract(start);
                if (result.signum() < 0) {
                    // ranges don't overlap
                    return BigDecimal.ZERO;
                }
                return end.subtract(start);
            }
        }

        private BigDecimal getEnd() {
            return end;
        }

        private BigDecimal getStart() {
            return start;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (arc == null ? 0 : arc.hashCode());
            result = prime * result + (end == null ? 0 : end.hashCode());
            result = prime * result + (start == null ? 0 : start.hashCode());
            return result;
        }

        protected boolean isEmpty() {
            return empty;
        }

        protected boolean isFull() {
            return full;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("ArcRange [arc=").append(arc).append(", start=").append(start)
                    .append(", end=").append(end).append("]");
            return builder.toString();
        }

    }

    public static class Builder {

        private static final OccupationTracker EMPTY = new OccupationTracker();

        public static OccupationTracker empty() {
            return Builder.EMPTY;
        }

        private final Directed directed;
        private final Deque<ArcRange> ranges = new ArrayDeque<ArcRange>();

        public Builder(final Directed d) {
            directed = d;
        }

        public void add(final Arc a, final BigDecimal start, final BigDecimal end) {
            this.add(create(a, start, end));
        }

        private void add(final ArcRange range) {
            if (directed.isEastbound()) {
                ranges.addLast(range);
            } else {
                ranges.addFirst(range);
            }
        }

        public void addFrom(final Arc a, final BigDecimal start) {
            this.add(create(a, start, a.getLength()));
        }

        public void addTo(final Arc a, final BigDecimal end) {
            this.add(create(a, BigDecimal.ZERO, end));
        }

        public void addWhole(final Arc a) {
            this.add(new ArcRange(a));
        }

        public OccupationTracker build() {
            return new OccupationTracker(ranges.toArray(new ArcRange[ranges.size()]));
        }

        protected ArcRange create(final Arc a, final BigDecimal start, final BigDecimal end) {
            if (directed.isEastbound()) {
                return new ArcRange(a, start, end);
            } else {
                return new ArcRange(a, a.getLength().subtract(end), a.getLength().subtract(start));
            }
        }
    }

    private final Map<Arc, ArcRange> ranges = new HashMap<Arc, ArcRange>();

    private OccupationTracker(final ArcRange... arcRanges) {
        for (final ArcRange range : arcRanges) {
            ranges.put(range.getArc(), range);
        }
    }

    public BigDecimal getConflictingMileage(final OccupationTracker other) {
        BigDecimal mileage = BigDecimal.ZERO;
        if (isEmpty() || other.isEmpty()) {
            return mileage;
        }
        final Collection<Arc> otherIncludedArcs = other.getIncludedArcs();
        for (final Arc a : getIncludedArcs()) {
            if (!otherIncludedArcs.contains(a)) {
                continue;
            }
            mileage = mileage.add(getRange(a).getConflictingMileage(other.getRange(a)));
        }
        return mileage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OccupationTracker)) {
            return false;
        }
        OccupationTracker other = (OccupationTracker) obj;
        if (ranges == null) {
            if (other.ranges != null) {
                return false;
            }
        } else if (!ranges.equals(other.ranges)) {
            return false;
        }
        return true;
    }

    public Collection<Arc> getIncludedArcs() {
        return ranges.keySet();
    }

    private ArcRange getRange(final Arc a) {
        return ranges.get(a);
    }

    public boolean isEmpty() {
        return ranges.size() == 0;
    }

}

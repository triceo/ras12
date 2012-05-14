package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.drools.planner.examples.ras2012.Directed;
import org.drools.planner.examples.ras2012.model.original.Arc;

public class OccupationTracker {

    protected static final class ArcRange {

        private final Arc arc;

        private final boolean full, empty;

        private final BigDecimal start, end;

        public ArcRange(final Arc a) {
            this(a, BigDecimal.ZERO, a.getLengthInMiles());
        }

        public ArcRange(final Arc a, final BigDecimal start, final BigDecimal end) {
            if (a == null) {
                throw new IllegalArgumentException("Arc must not be null.");
            }
            this.arc = a;
            if (start.signum() < 0) {
                throw new IllegalArgumentException("Arc range start must be >= 0.");
            }
            this.start = start;
            if (end.signum() < 0 || end.compareTo(a.getLengthInMiles()) > 0) {
                throw new IllegalArgumentException("Arc range end must be in the range of <0,"
                        + a.getLengthInMiles() + ">.");
            }
            this.end = end;
            this.full = start.equals(BigDecimal.ZERO) && end.equals(a.getLengthInMiles());
            this.empty = start.equals(end);
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
            if (this.arc != other.arc) {
                return false;
            }
            if (this.end == null) {
                if (other.end != null) {
                    return false;
                }
            } else if (!this.end.equals(other.end)) {
                return false;
            }
            if (this.start == null) {
                if (other.start != null) {
                    return false;
                }
            } else if (!this.start.equals(other.start)) {
                return false;
            }
            return true;
        }

        public Arc getArc() {
            return this.arc;
        }

        public BigDecimal getConflictingMileage(final ArcRange other) {
            if (this.getArc() != other.getArc()) {
                // ranges cannot conflict when they're of two different arcs
                return BigDecimal.ZERO;
            } else if (this.isFull() && other.isEmpty()) {
                // the intersection is empty
                return BigDecimal.ZERO;
            } else if (other.isFull() && this.isEmpty()) {
                // the intersection is empty
                return BigDecimal.ZERO;
            } else if (this.isFull() && other.isFull()) {
                // full conflict
                return this.getArc().getLengthInMiles();
            } else {
                // we need to calculate the actual conflicting range
                final BigDecimal start = this.getStart().max(other.getStart());
                final BigDecimal end = this.getEnd().min(other.getEnd());
                final BigDecimal result = end.subtract(start);
                if (result.signum() < 0) {
                    // ranges don't overlap
                    return BigDecimal.ZERO;
                }
                return end.subtract(start);
            }
        }

        private BigDecimal getEnd() {
            return this.end;
        }

        private BigDecimal getStart() {
            return this.start;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.arc == null ? 0 : this.arc.hashCode());
            result = prime * result + (this.end == null ? 0 : this.end.hashCode());
            result = prime * result + (this.start == null ? 0 : this.start.hashCode());
            return result;
        }

        private boolean isEmpty() {
            return this.empty;
        }

        private boolean isFull() {
            return this.full;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("ArcRange [arc=").append(this.arc).append(", start=").append(this.start)
                    .append(", end=").append(this.end).append("]");
            return builder.toString();
        }

    }

    public static class Builder {

        private static final OccupationTracker EMPTY = new OccupationTracker();

        public static OccupationTracker empty() {
            return Builder.EMPTY;
        }

        private final Directed        directed;
        private final Deque<ArcRange> ranges = new ArrayDeque<ArcRange>();

        public Builder(final Directed d) {
            this.directed = d;
        }

        public void add(final Arc a, final BigDecimal start, final BigDecimal end) {
            this.add(this.create(a, start, end));
        }

        private void add(final ArcRange range) {
            if (this.directed.isEastbound()) {
                this.ranges.addLast(range);
            } else {
                this.ranges.addFirst(range);
            }
        }

        public void addFrom(final Arc a, final BigDecimal start) {
            this.add(this.create(a, start, a.getLengthInMiles()));
        }

        public void addTo(final Arc a, final BigDecimal end) {
            this.add(this.create(a, BigDecimal.ZERO, end));
        }

        public void addWhole(final Arc a) {
            this.add(new ArcRange(a));
        }

        public OccupationTracker build() {
            return new OccupationTracker(this.ranges.toArray(new ArcRange[this.ranges.size()]));
        }

        protected ArcRange create(final Arc a, final BigDecimal start, final BigDecimal end) {
            if (this.directed.isEastbound()) {
                return new ArcRange(a, start, end);
            } else {
                return new ArcRange(a, a.getLengthInMiles().subtract(end), a.getLengthInMiles()
                        .subtract(start));
            }
        }
    }

    private final Map<Arc, ArcRange> ranges = new HashMap<Arc, ArcRange>();

    private OccupationTracker(final ArcRange... arcRanges) {
        for (final ArcRange range : arcRanges) {
            this.ranges.put(range.getArc(), range);
        }
    }

    public BigDecimal getConflictingMileage(final OccupationTracker other) {
        BigDecimal mileage = BigDecimal.ZERO;
        if (this.isEmpty() || other.isEmpty()) {
            return mileage;
        }
        final Collection<Arc> otherIncludedArcs = other.getIncludedArcs();
        for (final Arc a : this.getIncludedArcs()) {
            if (!otherIncludedArcs.contains(a)) {
                continue;
            }
            mileage = mileage.add(this.getRange(a).getConflictingMileage(other.getRange(a)));
        }
        return mileage;
    }

    public Collection<Arc> getIncludedArcs() {
        return this.ranges.keySet();
    }

    private ArcRange getRange(final Arc a) {
        return this.ranges.get(a);
    }

    public boolean isEmpty() {
        return this.ranges.size() == 0;
    }

}

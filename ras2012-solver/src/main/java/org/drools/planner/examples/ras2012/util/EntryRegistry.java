package org.drools.planner.examples.ras2012.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Train;

public class EntryRegistry {

    private static class RegistryItem {

        private final Map<Train, Pair<Long, Long>> timesByTrain = new TreeMap<Train, Pair<Long, Long>>();

        public int getConflicts() {
            int conflicts = 0;
            final long millisToAdd = TimeUnit.MINUTES.toMillis(5) - 1;
            for (Train train : timesByTrain.keySet()) {
                final Pair<Long, Long> entries = timesByTrain.get(train);
                final long forbiddenEntryWindowStart = entries.getLeft();
                final long forbiddenEntryWindowEnd = entries.getRight() + millisToAdd;
                final Range<Long> r = Range.between(forbiddenEntryWindowStart,
                        forbiddenEntryWindowEnd);
                for (Train otherTrain : timesByTrain.keySet()) {
                    if (train == otherTrain) {
                        // don't look for conflicts with itself
                        continue;
                    }
                    final Pair<Long, Long> otherEntries = timesByTrain.get(otherTrain);
                    final long trainEntry = otherEntries.getLeft();
                    final long trainLeave = otherEntries.getRight();
                    final Range<Long> r2 = Range.between(trainEntry, trainLeave);
                    if (r.isOverlappedBy(r2)) {
                        conflicts++;
                    }
                }
            }
            return conflicts;
        }

        public boolean resetTimes(final Train t) {
            final Pair<Long, Long> toRemove = this.timesByTrain.remove(t);
            if (toRemove != null) {
                return true;
            }
            return false;
        }

        /**
         * Set arcs occupied by the given train.
         * 
         * @param t
         * @param arcs
         * @return True if this change altered the state of the item.
         */
        public boolean setTimes(final Train t, final long entryTime, final long leaveTime) {
            final Pair<Long, Long> times = Pair.of(entryTime, leaveTime);
            if (times.equals(this.timesByTrain.get(t))) {
                return false;
            }
            this.timesByTrain.put(t, times);
            return true;
        }

    }

    private final Map<Arc, RegistryItem> items;

    public EntryRegistry(final int numberOfItems) {
        this.items = new HashMap<Arc, RegistryItem>(numberOfItems);
    }

    public int countConflicts() {
        int conflicts = 0;
        for (final RegistryItem item : this.items.values()) {
            conflicts += item.getConflicts();
        }
        return conflicts;
    }

    public void resetTimes(final Train t) {
        for (final RegistryItem item : this.items.values()) {
            item.resetTimes(t);
        }
    }

    public void setTimes(final Arc arc, final Train t, final long entryTime, final long leaveTime) {
        if (entryTime == leaveTime) {
            throw new IllegalArgumentException("Entry time cannot equal leave time!");
        }
        final boolean itemExists = this.items.containsKey(arc);
        final RegistryItem item = itemExists ? this.items.get(arc) : new RegistryItem();
        item.setTimes(t, entryTime, leaveTime);
        if (!itemExists) {
            this.items.put(arc, item);
        }
    }
}

package org.drools.planner.examples.ras2012.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            final List<Pair<Long, Long>> times = new ArrayList<Pair<Long, Long>>(
                    this.timesByTrain.values());
            final int size = times.size();
            final long millisToAdd = TimeUnit.MINUTES.toMillis(5);
            for (int train = 0; train < size; train++) {
                final Pair<Long, Long> entries = times.get(train);
                final long forbiddenEntryWindowStart = entries.getLeft();
                final long forbiddenEntryWindowEnd = entries.getRight() + millisToAdd;
                final Range<Long> r = Range.between(forbiddenEntryWindowStart,
                        forbiddenEntryWindowEnd);
                for (int otherTrain = 0; otherTrain < size; otherTrain++) {
                    if (train == otherTrain) {
                        // don't look for conflicts with itself
                        continue;
                    }
                    final Pair<Long, Long> otherEntries = times.get(otherTrain);
                    final long trainEntry = otherEntries.getLeft();
                    if (r.contains(trainEntry)) {
                        conflicts++;
                    }
                    final long trainLeave = otherEntries.getRight();
                    if (r.contains(trainLeave)) {
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
        final boolean itemExists = this.items.containsKey(arc);
        final RegistryItem item = itemExists ? this.items.get(arc) : new RegistryItem();
        item.setTimes(t, entryTime, leaveTime);
        if (!itemExists) {
            this.items.put(arc, item);
        }
    }
}

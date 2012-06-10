package org.drools.planner.examples.ras2012.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Train;

public class EntryRegistry {

    private static class RegistryItem {

        private final Map<Train, Pair<Long, Long>> timesByTrain = new HashMap<Train, Pair<Long, Long>>();
        private final List<Pair<Long, Long>>       times        = new ArrayList<Pair<Long, Long>>();
        private int                                cache        = Integer.MAX_VALUE;
        private boolean                            isCacheValid = false;

        public int getConflicts() {
            if (this.isCacheValid) {
                return this.cache;
            }
            int conflicts = 0;
            final List<Pair<Long, Long>> times = new ArrayList<Pair<Long, Long>>(
                    this.timesByTrain.values());
            final int size = times.size();
            for (int position = 0; position < size; position++) {
                final long trainEntry = this.times.get(position).getLeft();
                for (int i = position + 1; i < size; i++) {
                    final long otherTrainLeave = this.times.get(i).getRight();
                    if (otherTrainLeave < 0) {
                        continue;
                    }
                    final long difference = Math.abs(trainEntry - otherTrainLeave);
                    if (difference <= TimeUnit.MINUTES.toMillis(5)) {
                        conflicts++;
                    }
                }
            }
            this.cache = conflicts;
            this.isCacheValid = true;
            return this.cache;
        }

        public boolean resetTimes(final Train t) {
            final Pair<Long, Long> toRemove = this.timesByTrain.remove(t);
            if (toRemove != null) {
                this.times.remove(toRemove);
                this.isCacheValid = false;
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
            this.isCacheValid = false;
            final Pair<Long, Long> previous = this.timesByTrain.put(t, times);
            if (previous != null) {
                this.times.remove(previous);
            }
            this.times.add(times);
            return true;
        }

    }

    private final RegistryItem[]     items;
    private int                      nextAvailableIndex = 0;
    private final Map<Node, Integer> itemsIndex         = new HashMap<Node, Integer>();

    public EntryRegistry(final int numberOfItems) {
        this.items = new RegistryItem[numberOfItems];
    }

    public int countConflicts() {
        int conflicts = 0;
        for (final RegistryItem item : this.items) {
            if (item == null) {
                continue;
            }
            conflicts += item.getConflicts();
        }
        return conflicts;
    }

    public void resetTimes(final Train t) {
        for (final RegistryItem item : this.items) {
            if (item == null) {
                continue;
            }
            item.resetTimes(t);
        }
    }

    public void setTimes(final Node node, final Train t, final long entryTime, final long leaveTime) {
        final boolean itemExists = this.itemsIndex.containsKey(node);
        final int index = itemExists ? this.itemsIndex.get(node) : this.nextAvailableIndex;
        final RegistryItem item = itemExists ? this.items[index] : new RegistryItem();
        item.setTimes(t, entryTime, leaveTime);
        if (!itemExists) {
            this.items[index] = item;
            this.itemsIndex.put(node, index);
            this.nextAvailableIndex++;
        }
    }
}

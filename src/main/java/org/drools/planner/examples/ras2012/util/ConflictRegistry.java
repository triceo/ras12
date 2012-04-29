package org.drools.planner.examples.ras2012.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Train;

public class ConflictRegistry {

    private static class ConflictRegistryItem {

        private final Map<Train, Collection<Arc>> occupiedArcs = new HashMap<Train, Collection<Arc>>();

        public int getConflicts() {
            int conflicts = 0;
            final Set<Arc> conflictingArcs = new HashSet<Arc>();
            for (final Collection<Arc> arcsOccupiedByTrain : this.occupiedArcs.values()) {
                for (final Arc a : arcsOccupiedByTrain) {
                    if (conflictingArcs.contains(a)) {
                        conflicts++;
                    } else {
                        conflictingArcs.add(a);
                    }
                }
            }
            return conflicts;
        }

        public void reset() {
            this.occupiedArcs.clear();
        }

        public void resetTrainData(final Train t) {
            this.occupiedArcs.remove(t);
        }

        public void setOccupiedArcs(final Train t, final Collection<Arc> arcs) {
            this.occupiedArcs.put(t, arcs);
        }

    }

    private final Map<Long, ConflictRegistryItem> items;

    public ConflictRegistry(final int numberOfItems) {
        this.items = new HashMap<Long, ConflictRegistryItem>(numberOfItems);
    }

    public int countConflicts() {
        int conflicts = 0;
        for (final ConflictRegistryItem item : this.items.values()) {
            conflicts += item.getConflicts();
        }
        return conflicts;
    }

    public void reset() {
        for (final ConflictRegistryItem item : this.items.values()) {
            item.reset();
        }
    }

    public void resetTrainData(final Train t) {
        for (final ConflictRegistryItem item : this.items.values()) {
            item.resetTrainData(t);
        }
    }

    public void setOccupiedArcs(final long time, final Train t, final Collection<Arc> occupiedArcs) {
        if (!this.items.containsKey(time)) {
            this.items.put(time, new ConflictRegistryItem());
        }
        this.items.get(time).setOccupiedArcs(t, occupiedArcs);
    }

}

package org.drools.planner.examples.ras2012.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker;

public class ConflictRegistry {

    private static class ConflictRegistryItem {

        private final Map<Train, OccupationTracker> occupiedArcsByTrain = new HashMap<Train, OccupationTracker>();
        private final List<OccupationTracker>       occupiedArcs        = new ArrayList<OccupationTracker>();

        public double getConflicts() {
            double conflicts = 0;
            final int size = this.occupiedArcs.size();
            for (int position = 0; position < size; position++) {
                final OccupationTracker left = this.occupiedArcs.get(position);
                for (int i = position + 1; i < size; i++) {
                    final OccupationTracker right = this.occupiedArcs.get(i);
                    conflicts += left.getConflictingMileage(right);
                }
            }
            return conflicts;
        }

        public void resetOccupiedArcs(final Train t) {
            final OccupationTracker toRemove = this.occupiedArcsByTrain.remove(t);
            if (toRemove != null) {
                this.occupiedArcs.remove(toRemove);
            }
        }

        public void setOccupiedArcs(final Train t, final OccupationTracker arcs) {
            final OccupationTracker previous = this.occupiedArcsByTrain.put(t, arcs);
            if (previous != null) {
                this.occupiedArcs.remove(previous);
            }
            this.occupiedArcs.add(arcs);
        }

    }

    private final ConflictRegistryItem[] items;
    private int                          nextAvailableIndex = 0;
    private final Map<Long, Integer>     itemsIndex         = new HashMap<Long, Integer>();

    public ConflictRegistry(final int numberOfItems) {
        this.items = new ConflictRegistryItem[numberOfItems];
    }

    public int countConflicts() {
        double conflicts = 0;
        for (final ConflictRegistryItem item : this.items) {
            if (item == null) {
                continue;
            }
            conflicts += item.getConflicts();
        }
        return (int) Math.round(conflicts);
    }

    public void setOccupiedArcs(final Long time, final Train t, final OccupationTracker occupiedArcs) {
        final boolean itemExists = this.itemsIndex.containsKey(time);
        final int index = itemExists ? this.itemsIndex.get(time) : this.nextAvailableIndex;
        if (occupiedArcs.isEmpty()) {
            /*
             * empty occupied arcs can cause no conflicts, ignore them; this will save some memory and a lot of looping later
             */
            if (itemExists) {
                // in a situation where there's already some occupied arcs set, we need to empty them
                this.items[index].resetOccupiedArcs(t);
            }
        } else {
            final ConflictRegistryItem item = itemExists ? this.items[index]
                    : new ConflictRegistryItem();
            item.setOccupiedArcs(t, occupiedArcs);
            if (!itemExists) {
                this.items[index] = item;
                this.itemsIndex.put(time, index);
                this.nextAvailableIndex++;
            }
        }
    }
}

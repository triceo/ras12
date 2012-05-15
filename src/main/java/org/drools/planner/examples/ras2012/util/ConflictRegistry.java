package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker;

public class ConflictRegistry {

    private static class ConflictRegistryItem {

        private final Map<Train, OccupationTracker> occupiedArcs = new HashMap<Train, OccupationTracker>();

        public BigDecimal getConflicts() {
            BigDecimal conflicts = BigDecimal.ZERO;
            final Collection<OccupationTracker> all = new HashSet<OccupationTracker>(
                    this.occupiedArcs.values());
            final Collection<OccupationTracker> unvisited = new HashSet<OccupationTracker>(all);
            for (final OccupationTracker oa : all) {
                unvisited.remove(oa);
                for (final OccupationTracker oa2 : unvisited) {
                    conflicts = conflicts.add(oa.getConflictingMileage(oa2));
                }
            }
            return conflicts;
        }

        public void resetOccupiedArcs(final Train t) {
            this.occupiedArcs.remove(t);
        }

        public void setOccupiedArcs(final Train t, final OccupationTracker arcs) {
            this.occupiedArcs.put(t, arcs);
        }

    }

    private final Map<Long, ConflictRegistryItem> items;

    public ConflictRegistry(final int numberOfItems) {
        this.items = new HashMap<Long, ConflictRegistryItem>(numberOfItems);
    }

    public int countConflicts() {
        BigDecimal conflicts = BigDecimal.ZERO;
        for (final ConflictRegistryItem item : this.items.values()) {
            conflicts = conflicts.add(item.getConflicts());
        }
        return conflicts.setScale(0, BigDecimal.ROUND_HALF_EVEN).intValue();
    }

    public void resetOccupiedArcs(final Train t) {
        for (final ConflictRegistryItem item : this.items.values()) {
            item.resetOccupiedArcs(t);
        }
    }

    public void setOccupiedArcs(final long time, final Train t, final OccupationTracker occupiedArcs) {
        if (!this.items.containsKey(time)) {
            this.items.put(time, new ConflictRegistryItem());
        }
        this.items.get(time).setOccupiedArcs(t, occupiedArcs);
    }

}

package org.drools.planner.examples.ras2012;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.simple.SimpleScoreCalculator;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.planner.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.planner.TrainConflict;

public class RAS2012ScoreCalculator implements SimpleScoreCalculator<RAS2012Solution> {

    private Set<TrainConflict> getConflicts(RAS2012Solution solution) {
        Set<TrainConflict> allConflicts = new HashSet<TrainConflict>();
        // insert the number of conflicts for the given assignments
        for (BigDecimal time = BigDecimal.ZERO; time.intValue() <= RAS2012Solution.PLANNING_HORIZON_MINUTES; time = time
                .add(RAS2012Solution.PLANNING_TIME_DIVISION_MINUTES)) {
            // for each point in time...
            final Map<Arc, Integer> arcUsage = new HashMap<Arc, Integer>();
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                // ... and each assignment...
                final Itinerary i = ia.getItinerary();
                for (final Arc a : i.getCurrentlyOccupiedArcs(time)) {
                    // ... find out how many times an arc has been used
                    if (arcUsage.containsKey(a)) {
                        arcUsage.put(a, arcUsage.get(a) + 1);
                    } else {
                        arcUsage.put(a, 1);
                    }
                }
            }
            int conflicts = 0;
            // when an arc has been used more than once, it is a conflict of two itineraries
            for (final int numOfUses : arcUsage.values()) {
                conflicts += numOfUses - 1;
            }
            allConflicts.add(new TrainConflict(time, conflicts));
        }
        return allConflicts;
    }

    @Override
    public HardAndSoftScore calculateScore(RAS2012Solution solution) {
        // count the number of conflicts
        int conflicts = 0;
        for (TrainConflict c : this.getConflicts(solution)) {
            conflicts += c.getNumConflicts();
        }
        HardAndSoftScore score = DefaultHardAndSoftScore.valueOf(-conflicts, 0);
        return score;
    }

}

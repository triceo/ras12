package org.drools.planner.examples.ras2012;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.simple.SimpleScoreCalculator;
import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.planner.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.planner.TrainConflict;

public class RAS2012ScoreCalculator implements SimpleScoreCalculator<RAS2012Solution> {

    private int roundMinutesToWholeHours(BigDecimal minutes) {
        BigDecimal hours = minutes.divide(BigDecimal.valueOf(60), 10, BigDecimal.ROUND_HALF_EVEN);
        int result = hours.setScale(0, BigDecimal.ROUND_UP).intValue();
        return result;
    }

    private boolean isInPlanningHorizon(BigDecimal time) {
        return time.compareTo(BigDecimal.valueOf(RAS2012Solution.PLANNING_HORIZON_MINUTES)) <= 0;
    }

    private int getCostPerItinerary(ScheduleProducer i) {
        int penalty = 0;
        /*
         * want time penalties are only counted when the train arrives on hour before or three hours after the want time
         */
        int hourlyDifference = 0;
        Map<BigDecimal, BigDecimal> wantTimeDifferences = i.getWantTimeDifference();
        for (Map.Entry<BigDecimal, BigDecimal> entry : wantTimeDifferences.entrySet()) {
            if (!isInPlanningHorizon(entry.getKey())) {
                // difference occured past the planning horizon; we don't care about it
                continue;
            }
            BigDecimal wantTimeDifference = entry.getValue();
            if (wantTimeDifference.signum() > 0) {
                int hours = roundMinutesToWholeHours(wantTimeDifference);
                if (hours > 3) {
                    hourlyDifference = hours - 3;
                }
            } else if (wantTimeDifference.signum() < 0) {
                int hours = roundMinutesToWholeHours(wantTimeDifference);
                if (hours < -1) {
                    hourlyDifference = Math.abs(hours + 1);
                }
            }
            penalty += hourlyDifference * 75;
        }
        /*
         * calculate hot schedule adherence penalties, given that the train needs to adhere and that the delay is more than 2
         * hours
         */
        if (i.getTrain().getType().adhereToSchedule()) {
            Map<BigDecimal, BigDecimal> sa = i.getScheduleAdherenceStatus();
            for (Map.Entry<BigDecimal, BigDecimal> entry : sa.entrySet()) {
                if (!isInPlanningHorizon(entry.getKey())) {
                    // difference occured past the planning horizon; we don't care about it
                    continue;
                }
                BigDecimal difference = entry.getValue();
                if (difference.signum() < 1) {
                    continue;
                }
                hourlyDifference = roundMinutesToWholeHours(difference);
                if (hourlyDifference > 2) {
                    penalty += (hourlyDifference - 2) * 200;
                }
            }
        }
        /*
         * calculate time spent on unpreferred tracks
         */
        penalty += roundMinutesToWholeHours(i.getTimeSpentOnUnpreferredTracks(BigDecimal
                .valueOf(RAS2012Solution.PLANNING_HORIZON_MINUTES))) * 50;
        return penalty;
    }

    private Set<TrainConflict> getConflicts(RAS2012Solution solution) {
        Set<TrainConflict> allConflicts = new HashSet<TrainConflict>();
        // insert the number of conflicts for the given assignments
        for (BigDecimal time = BigDecimal.ZERO; time.intValue() <= RAS2012Solution.PLANNING_HORIZON_MINUTES; time = time
                .add(RAS2012Solution.PLANNING_TIME_DIVISION_MINUTES)) {
            // for each point in time...
            final Map<Arc, Integer> arcUsage = new HashMap<Arc, Integer>();
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                // ... and each assignment...
                final ScheduleProducer i = ia.getItinerary();
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
        int cost = 0;
        for (ItineraryAssignment ia : solution.getAssignments()) {
            cost += this.getCostPerItinerary(ia.getItinerary());
        }
        HardAndSoftScore score = DefaultHardAndSoftScore.valueOf(-conflicts, -cost);
        return score;
    }

}

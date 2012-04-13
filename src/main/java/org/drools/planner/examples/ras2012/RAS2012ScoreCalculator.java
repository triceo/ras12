package org.drools.planner.examples.ras2012;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.simple.SimpleScoreCalculator;
import org.drools.planner.examples.ras2012.interfaces.ScheduleProducer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.WaitTime;

public class RAS2012ScoreCalculator implements SimpleScoreCalculator<RAS2012Solution> {

    @Override
    public HardAndSoftScore calculateScore(final RAS2012Solution solution) {
        // count the number of conflicts
        int penalty = 0;
        for (final ItineraryAssignment ia : solution.getAssignments()) {
            /*
             * want time penalties are only counted when the train arrives on hour before or three hours after the want time
             */
            penalty += this.getWantTimePenalty(ia.getItinerary(), solution);
            /*
             * calculate hot schedule adherence penalties, given that the train needs to adhere and that the delay is more than
             * 2 hours
             */
            penalty += this.getScheduleAdherencePenalty(ia.getItinerary(), solution);
            /*
             * calculate time spent on unpreferred tracks
             */
            penalty += this.roundMillisecondsToWholeHours(ia.getItinerary()
                    .getTimeSpentOnUnpreferredTracks(
                            RAS2012Solution.PLANNING_HORIZON_MINUTES * 60 * 1000)) * 50;
            /*
             * calculate penalty for delays on the route
             */
            penalty += this.getDelayPenalty(ia.getItinerary(), solution);
        }
        final HardAndSoftScore score = DefaultHardAndSoftScore.valueOf(
                -this.getConflicts(solution), -penalty);
        return score;
    }

    private int getConflicts(final RAS2012Solution solution) {
        int conflicts = 0;
        // insert the number of conflicts for the given assignments
        for (long milliseconds = 0; this.isInPlanningHorizon(milliseconds); milliseconds += 30000) {
            // for each point in time...
            final Map<Arc, Integer> arcConflicts = new HashMap<Arc, Integer>();
            for (final ItineraryAssignment ia : solution.getAssignments()) {
                // ... and each assignment...
                final ScheduleProducer i = ia.getItinerary();
                for (final Arc a : i.getCurrentlyOccupiedArcs(milliseconds)) {
                    // ... find out how many times an arc has been used
                    if (arcConflicts.containsKey(a)) {
                        arcConflicts.put(a, arcConflicts.get(a) + 1);
                    } else {
                        arcConflicts.put(a, 0);
                    }
                }
            }
            // when an arc has been used more than once, it is a conflict of two itineraries
            for (final Map.Entry<Arc, Integer> entry : arcConflicts.entrySet()) {
                conflicts += entry.getValue();
            }
        }
        return conflicts;
    }

    private int getDelayPenalty(final ScheduleProducer i, final RAS2012Solution solution) {
        long delay = 0;
        for (final SortedMap.Entry<Long, Node> entry : i.getSchedule().entrySet()) {
            if (!this.isInPlanningHorizon(entry.getKey())) {
                // outside planning horizon
                break;
            }
            final WaitTime wt = i.getWaitTime(entry.getValue());
            if (wt == null) {
                // no delay in the node
                continue;
            }
            delay += wt.getMillisWaitFor();
        }
        final int hoursDelay = this.roundMillisecondsToWholeHours(delay);
        return Math.max(0, hoursDelay) * i.getTrain().getType().getDelayPenalty();
    }

    private int getScheduleAdherencePenalty(final ScheduleProducer i, final RAS2012Solution solution) {
        int penalty = 0;
        if (i.getTrain().getType().adhereToSchedule()) {
            final Map<Long, Long> sa = i.getScheduleAdherenceStatus();
            for (final Map.Entry<Long, Long> entry : sa.entrySet()) {
                if (!this.isInPlanningHorizon(entry.getKey())) {
                    // difference occured past the planning horizon; we don't care about it
                    continue;
                }
                final long difference = entry.getValue();
                if (difference < 1) {
                    continue;
                }
                final int hourlyDifference = this.roundMillisecondsToWholeHours(difference);
                if (hourlyDifference > 2) {
                    penalty += (hourlyDifference - 2) * 200;
                }
            }
        }
        return penalty;
    }

    private int getWantTimePenalty(final ScheduleProducer i, final RAS2012Solution solution) {
        int penalty = 0;
        final Map<Long, Long> wantTimeDifferences = i.getWantTimeDifference();
        for (final Map.Entry<Long, Long> entry : wantTimeDifferences.entrySet()) {
            int hourlyDifference = 0;
            if (!this.isInPlanningHorizon(entry.getKey())) {
                // difference occured past the planning horizon; we don't care about it
                continue;
            }
            final long wantTimeDifference = entry.getValue();
            if (wantTimeDifference > 0) {
                final int hours = this.roundMillisecondsToWholeHours(wantTimeDifference);
                if (hours > 3) {
                    hourlyDifference = hours - 3;
                }
            } else if (wantTimeDifference < 0) {
                final int hours = this.roundMillisecondsToWholeHours(wantTimeDifference);
                if (hours < -1) {
                    hourlyDifference = Math.abs(hours + 1);
                }
            }
            penalty += hourlyDifference * 75;
        }
        return penalty;
    }

    private boolean isInPlanningHorizon(final long time) {
        final long horizon = RAS2012Solution.PLANNING_HORIZON_MINUTES * 60 * 1000;
        return time < horizon;
    }

    private int roundMillisecondsToWholeHours(final long milliseconds) {
        int result = (int) Math.ceil(Math.abs(milliseconds) / 1000.0 / 60.0 / 60.0);
        if (milliseconds < 0) {
            return -result;
        } else {
            return result;
        }
    }
}

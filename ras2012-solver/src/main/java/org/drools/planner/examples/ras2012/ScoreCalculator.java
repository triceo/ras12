package org.drools.planner.examples.ras2012;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.incremental.AbstractIncrementalScoreCalculator;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.util.ConflictRegistry;
import org.drools.planner.examples.ras2012.util.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCalculator extends AbstractIncrementalScoreCalculator<ProblemSolution> {

    private static final Logger     logger                       = LoggerFactory
                                                                         .getLogger(ScoreCalculator.class);

    private static final int        OCCUPATION_CHECKS_PER_MINUTE = 2;

    private static final BigDecimal MILLIS_TO_HOURS              = BigDecimal.valueOf(3600000);

    public static HardAndSoftScore oneTimeCalculation(final ProblemSolution solution) {
        final ScoreCalculator calc = new ScoreCalculator();
        calc.resetWorkingSolution(solution);
        return calc.calculateScore();
    }

    private static BigDecimal roundMillisecondsToHours(final long milliseconds) {
        return BigDecimal.valueOf(milliseconds).divide(ScoreCalculator.MILLIS_TO_HOURS,
                Converter.BIGDECIMAL_SCALE, Converter.BIGDECIMAL_ROUNDING);
    }

    private ProblemSolution           solution                   = null;

    private final Map<Train, Integer> wantTimePenalties          = new HashMap<Train, Integer>();

    private final Map<Train, Integer> delayPenalties             = new HashMap<Train, Integer>();

    private final Map<Train, Integer> scheduleAdherencePenalties = new HashMap<Train, Integer>();

    private final Map<Train, Integer> unpreferredTracksPenalties = new HashMap<Train, Integer>();

    private ConflictRegistry          conflicts;

    @Override
    public void afterAllVariablesChanged(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.insert((ItineraryAssignment) entity);
        }
    }

    @Override
    public void afterEntityAdded(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.insert((ItineraryAssignment) entity);
        }
    }

    @Override
    public void afterEntityRemoved(final Object entity) {
        // do nothing
    }

    @Override
    public void afterVariableChanged(final Object entity, final String variableName) {
        if (entity instanceof ItineraryAssignment) {
            this.insert((ItineraryAssignment) entity);
        }
    }

    @Override
    public void beforeAllVariablesChanged(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.retract((ItineraryAssignment) entity);
        }
    }

    @Override
    public void beforeEntityAdded(final Object entity) {
        // do nothing
    }

    @Override
    public void beforeEntityRemoved(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.retract((ItineraryAssignment) entity);
        }
    }

    @Override
    public void beforeVariableChanged(final Object entity, final String variableName) {
        if (entity instanceof ItineraryAssignment) {
            this.retract((ItineraryAssignment) entity);
        }
    }

    @Override
    public HardAndSoftScore calculateScore() {
        ScoreCalculator.logger.debug("Calculating score.");
        int penalty = 0;
        for (final Train t : this.solution.getTrains()) {
            penalty += this.wantTimePenalties.get(t);
            penalty += this.delayPenalties.get(t);
            penalty += this.scheduleAdherencePenalties.get(t);
            penalty += this.unpreferredTracksPenalties.get(t);
        }
        final int conflicts = this.conflicts.countConflicts();
        if (conflicts > 0) {
            return DefaultHardAndSoftScore.valueOf(-conflicts, -penalty);
        } else {
            return DefaultHardAndSoftScore.valueOf(0, -penalty);
        }
    }

    private void clearEveryCache() {
        this.wantTimePenalties.clear();
        this.unpreferredTracksPenalties.clear();
        this.scheduleAdherencePenalties.clear();
        this.delayPenalties.clear();
        this.conflicts = new ConflictRegistry(
                (int) this.solution.getPlanningHorizon(TimeUnit.MINUTES)
                        * ScoreCalculator.OCCUPATION_CHECKS_PER_MINUTE);
    }

    public int getDelayPenalty(final Itinerary i) {
        final long delay = i.getDelay();
        if (delay <= 0) {
            return 0;
        }
        final BigDecimal hoursDelay = ScoreCalculator.roundMillisecondsToHours(delay);
        final BigDecimal maxHoursDelay = hoursDelay.max(BigDecimal.ZERO);
        return maxHoursDelay.multiply(BigDecimal.valueOf(i.getTrain().getType().getDelayPenalty()))
                .intValue();
    }

    public int getScheduleAdherencePenalty(final Itinerary i) {
        int penalty = 0;
        if (i.getTrain().getType().adhereToSchedule()) {
            for (final Node node : i.getTrain().getScheduleAdherenceRequirements().keySet()) {
                penalty += this.getScheduleAdherencePenalty(i, node);
            }
        }
        return penalty;
    }

    public int getScheduleAdherencePenalty(final Itinerary i, final Node node) {
        if (!i.getTrain().getType().adhereToSchedule()) {
            return 0;
        }
        final long arrival = i.getArrivalTime(node);
        if (!this.isInPlanningHorizon(arrival)) {
            return 0;
        }
        final long expectedArrival = i.getTrain().getScheduleAdherenceRequirements().get(node)
                .getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS);
        if (arrival <= expectedArrival) {
            return 0;
        }
        final long difference = arrival - expectedArrival;
        BigDecimal hourlyDifference = ScoreCalculator.roundMillisecondsToHours(difference);
        hourlyDifference = hourlyDifference.subtract(BigDecimal.valueOf(2));
        if (hourlyDifference.signum() > 0) {
            return hourlyDifference.multiply(BigDecimal.valueOf(200)).intValue();
        }
        return 0;
    }

    public int getUnpreferredTracksPenalty(final Itinerary i) {
        final BigDecimal hours = ScoreCalculator.roundMillisecondsToHours(i
                .getTimeSpentOnUnpreferredTracks(this.solution
                        .getPlanningHorizon(TimeUnit.MILLISECONDS)));
        return hours.multiply(BigDecimal.valueOf(50)).intValue();
    }

    public int getWantTimePenalty(final Itinerary i) {
        final long actualTime = i.getArrivalTime();
        if (!this.isInPlanningHorizon(actualTime)) {
            // arrivals outside of the planning horizon aren't counted
            return 0;
        }
        final long delay = actualTime - i.getTrain().getWantTime(TimeUnit.MILLISECONDS);
        BigDecimal hours = ScoreCalculator.roundMillisecondsToHours(delay);
        final BigDecimal penalty = BigDecimal.valueOf(75);
        if (delay > 0) {
            hours = hours.subtract(BigDecimal.valueOf(3));
            if (hours.signum() > 0) {
                return hours.multiply(penalty).intValue();
            }
        } else if (delay < 0) {
            hours = hours.add(BigDecimal.valueOf(1));
            if (hours.signum() < 0) {
                return -hours.multiply(penalty).intValue();
            }
        }
        return 0;
    }

    private void insert(final ItineraryAssignment ia) {
        ScoreCalculator.logger.debug("Inserting entity: " + ia);
        final Train t = ia.getTrain();
        final Itinerary i = ia.getItinerary();
        this.unpreferredTracksPenalties.put(t, this.getUnpreferredTracksPenalty(i));
        this.scheduleAdherencePenalties.put(t, this.getScheduleAdherencePenalty(i));
        this.wantTimePenalties.put(t, this.getWantTimePenalty(i));
        this.delayPenalties.put(t, this.getDelayPenalty(i));
        this.recalculateOccupiedArcs(ia);
    }

    private boolean isInPlanningHorizon(final long time) {
        return time <= this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
    }

    /**
     * if it were possible for a train to have at the same time both entrytime > 0 and origin != depo, this code would miss the
     * arcs occupied by the train before it "magically" appeared in the middle of the territory.
     */
    private void recalculateOccupiedArcs(final ItineraryAssignment ia) {
        final int scanEveryXMillis = 60000 / ScoreCalculator.OCCUPATION_CHECKS_PER_MINUTE;
        // insert the number of conflicts for the given assignments
        final Itinerary i = ia.getItinerary();
        final Train t = ia.getTrain();
        final long startingTime = Math.max(0, i.getArrivalTime(t.getOrigin()));
        final long endingTime = Math.min(i.getLeaveTime(t.getDestination()),
                this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS));
        for (long time = startingTime; time <= endingTime; time += scanEveryXMillis) {
            this.conflicts.setOccupiedArcs(time, t, i.getOccupiedArcs(time));
        }
    }

    @Override
    public void resetWorkingSolution(final ProblemSolution workingSolution) {
        ScoreCalculator.logger.debug("Resetting working solution.");
        this.solution = workingSolution;
        this.clearEveryCache();
        for (final ItineraryAssignment ia : this.solution.getAssignments()) {
            this.insert(ia);
        }
    }

    private void retract(final ItineraryAssignment ia) {
        ScoreCalculator.logger.debug("Removing entity: " + ia);
        final Train t = ia.getTrain();
        this.wantTimePenalties.remove(t);
        this.unpreferredTracksPenalties.remove(t);
        this.scheduleAdherencePenalties.remove(t);
        this.delayPenalties.remove(t);
        this.conflicts.resetOccupiedArcs(t);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ScoreCalculator, TWT: ");
        for (final Map.Entry<Train, Integer> entry : this.wantTimePenalties.entrySet()) {
            sb.append(entry.getKey().getName()).append(">").append(entry.getValue()).append("; ");
        }
        sb.append(System.lineSeparator()).append("SA:  ");
        for (final Map.Entry<Train, Integer> entry : this.scheduleAdherencePenalties.entrySet()) {
            sb.append(entry.getKey().getName()).append(">").append(entry.getValue()).append("; ");
        }
        sb.append(System.lineSeparator()).append("+-:  ");
        for (final Map.Entry<Train, Integer> entry : this.delayPenalties.entrySet()) {
            sb.append(entry.getKey().getName()).append(">").append(entry.getValue()).append("; ");
        }
        sb.append(System.lineSeparator()).append("Prf: ");
        for (final Map.Entry<Train, Integer> entry : this.unpreferredTracksPenalties.entrySet()) {
            sb.append(entry.getKey().getName()).append(">").append(entry.getValue()).append("; ");
        }
        return sb.append(System.lineSeparator()).toString();
    }
}

package org.drools.planner.examples.ras2012;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.incremental.AbstractIncrementalScoreCalculator;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.Itinerary.ChangeType;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;
import org.drools.planner.examples.ras2012.util.ConflictRegistry;
import org.drools.planner.examples.ras2012.util.Converter;
import org.drools.planner.examples.ras2012.util.EntryRegistry;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker;

/**
 * <p>
 * A class that converts a solution into a score, in order to be able to rank various solutions based on their efficiency. For
 * performance reasons, this class implements incremental score calculation mechanisms. The important bits:
 * </p>
 * 
 * <ul>
 * <li>When {@link #resetWorkingSolution(ProblemSolution)} is called, this class is reset. No deltas are kept, we're starting
 * fresh.</li>
 * <li>Call to any of the <code>after...</code> or <code>before...</code> methods lets us know that some entity has changed.
 * Data associated with that entity need to be re-calculated. Cached data from other entities won't be touched.</li>
 * <li>When {@link #calculateScore()} is called, all that data is summed up and the result is the score for that particular
 * solution.</li>
 * </ul>
 * 
 * <p>
 * Each score has two parts, a hard score and a soft score. Hard score, if negative, means that there are some constraints
 * broken with which the solution doesn't make sense. These constraints are:
 * </p>
 * 
 * <dl>
 * <dt>Occupied Arcs (see {@link #recalculateOccupiedArcs(ItineraryAssignment)})</dt>
 * <dd>When train is on a route, it occupies certain arcs. If any other train occupies the same arcs at the given time, there
 * will be a collision.</dd>
 * <dt>Entry times (see {@link #recalculateEntries(ItineraryAssignment)})</dt>
 * <dd>The problem definition requires that train enters an arc no sooner than 5 minutes after it's been cleared by the previous
 * train occupying it.</dd>
 * </dl>
 * 
 * <p>
 * Soft constraints, those that only affect score quality and not its feasibility, are all defined by the problem. They are:
 * </p>
 * 
 * <ul>
 * <li>Train's delay on the route (see {@link #getDelayPenalty(Itinerary)}),</li>
 * <li>terminal want time (see {@link #getWantTimePenalty(Itinerary)}),</li>
 * <li>schedule adherence (see {@link #getScheduleAdherencePenalty(Itinerary)})</li>
 * <li>and time spent on unpreferred tracks (see {@link #getUnpreferredTracksPenalty(Itinerary)}).</li>
 * </ul>
 * 
 */
public class ScoreCalculator extends AbstractIncrementalScoreCalculator<ProblemSolution> {

    /**
     * How often during the planning horizon should occupied arcs be calculated. The higher this value, the more certainty that
     * no trains will scrape others. However, there is an inverse relation between this number and algorithm's performance. See
     * {@link #recalculateOccupiedArcs(ItineraryAssignment)} for more information.
     */
    private static final int        OCCUPATION_CHECKS_PER_MINUTE = 2;

    private static final BigDecimal MILLIS_TO_HOURS              = BigDecimal.valueOf(3600000);

    /**
     * Perform a one-time calculation on a given solution. This eliminates the possible side-effects of incremental score
     * calculation, resulting in a score that is guaranteed to be correct.
     * 
     * @param solution Solution to calculate the score for.
     * @return Score of the given solution.
     */
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

    private EntryRegistry             entries;

    @Override
    public void afterAllVariablesChanged(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.modify((ItineraryAssignment) entity);
        }
    }

    @Override
    public void afterEntityAdded(final Object entity) {
        if (entity instanceof ItineraryAssignment) {
            this.modify((ItineraryAssignment) entity);
        }
    }

    @Override
    public void afterEntityRemoved(final Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void afterVariableChanged(final Object entity, final String variableName) {
        if (entity instanceof ItineraryAssignment) {
            this.modify((ItineraryAssignment) entity);
        }
    }

    @Override
    public void beforeAllVariablesChanged(final Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void beforeEntityAdded(final Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void beforeEntityRemoved(final Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void beforeVariableChanged(final Object entity, final String variableName) {
        throw new NotImplementedException();
    }

    /**
     * Calculate the score of the solution after the increments have been resolved. This methods only sums up those increments,
     * except in cases where we need to take into account the information from all the trains. These cases are:
     * 
     * <ul>
     * <li>Conflicts in occupied arcs are calculated in {@link ConflictRegistry}.</li>
     * <li>Conflicts in entry times are calculated in {@link EntryRegistry}.</li>
     * </ul>
     */
    @Override
    public HardAndSoftScore calculateScore() {
        int penalty = 0;
        for (final Train t : this.solution.getTrains()) {
            penalty += this.wantTimePenalties.get(t);
            penalty += this.delayPenalties.get(t);
            penalty += this.scheduleAdherencePenalties.get(t);
            penalty += this.unpreferredTracksPenalties.get(t);
        }
        final int conflicts = this.entries.countConflicts() + this.conflicts.countConflicts();
        return DefaultHardAndSoftScore.valueOf(-conflicts, -penalty);
    }

    /**
     * Calculate delay penalty for a given schedule. The rules for that are defined by the RAS 2012 problem statement.
     * 
     * @param i The schedule in question.
     * @return The penalty in dollars.
     */
    public int getDelayPenalty(final Itinerary i) {
        final long delay = i.getDelay(this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS));
        if (delay <= 0) {
            return 0;
        }
        final BigDecimal hoursDelay = ScoreCalculator.roundMillisecondsToHours(delay);
        final BigDecimal maxHoursDelay = hoursDelay.max(BigDecimal.ZERO);
        return maxHoursDelay.multiply(BigDecimal.valueOf(i.getTrain().getType().getDelayPenalty()))
                .intValue();
    }

    /**
     * Retrieve the first point in time where the particular schedule has seen updates. This is used to determine the point from
     * which to re-calculate occupied arcs. See {@link Itinerary#getLatestWaitTimeChange()} for details.
     * 
     * @param i The schedule in question.
     * @return The point in time in milliseconds from which the occupied arcs need to be recalculated.
     */
    private long getFirstChangeTime(final Itinerary i) {
        final Pair<ChangeType, Node> lastChange = i.getLatestWaitTimeChange();
        switch (lastChange.getLeft()) {
            case REMOVE_WAIT_TIME:
            case SET_WAIT_TIME:
                final Node previousToModifiedNode = i.getRoute().getProgression()
                        .getPreviousNode(lastChange.getRight());
                if (previousToModifiedNode != null && i.hasNode(previousToModifiedNode)) {
                    // start re-calculating occupied arcs from the first change in the itinerary
                    return i.getArrivalTime(previousToModifiedNode);
                }
            default:
                // re-calculate arcs all across the timeline
                return i.getArrivalTime(i.getTrain().getOrigin());
        }
    }

    /**
     * Calculate schedule adherence penalty for all nodes on a schedule. The rules for that are defined by the RAS 2012 problem
     * statement.
     * 
     * @param i The schedule in question.
     * @return The penalty in dollars.
     */
    public int getScheduleAdherencePenalty(final Itinerary i) {
        int penalty = 0;
        if (i.getTrain().getType().adhereToSchedule()) {
            for (final Node node : i.getTrain().getScheduleAdherenceRequirements().keySet()) {
                penalty += this.getScheduleAdherencePenalty(i, node);
            }
        }
        return penalty;
    }

    /**
     * Calculate schedule adherence penalty for a given node on a schedule. The rules for that are defined by the RAS 2012
     * problem statement.
     * 
     * @param i The schedule in question.
     * @param node The node in question.
     * @return The penalty in dollars.
     */
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

    /**
     * Calculate a penalty for using unpreferred tracks in a given schedule. The rules for that are defined by the RAS 2012
     * problem statement.
     * 
     * @param i The schedule in question.
     * @return The penalty in dollars.
     */
    public int getUnpreferredTracksPenalty(final Itinerary i) {
        final BigDecimal hours = ScoreCalculator.roundMillisecondsToHours(i
                .getTimeSpentOnUnpreferredTracks(this.solution
                        .getPlanningHorizon(TimeUnit.MILLISECONDS)));
        return hours.multiply(BigDecimal.valueOf(50)).intValue();
    }

    /**
     * Calculate a want time penalty for a given schedule. The rules for that are defined by the RAS 2012 problem statement.
     * 
     * @param i The schedule in question.
     * @return The penalty in dollars.
     */
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

    /**
     * Whether or not a given time falls into the planning horizon.
     * 
     * @param time The time in question.
     * @return True if 0 <= time <= horizon, false otherwise.
     */
    private boolean isInPlanningHorizon(final long time) {
        if (time < 0) {
            return false;
        }
        return time <= this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
    }

    /**
     * Re-calculate constraints for the changed entity.
     * 
     * @param ia The entity that's changed.
     */
    private void modify(final ItineraryAssignment ia) {
        final Train t = ia.getTrain();
        final Itinerary i = ia.getItinerary();
        this.unpreferredTracksPenalties.put(t, this.getUnpreferredTracksPenalty(i));
        this.scheduleAdherencePenalties.put(t, this.getScheduleAdherencePenalty(i));
        this.wantTimePenalties.put(t, this.getWantTimePenalty(i));
        this.delayPenalties.put(t, this.getDelayPenalty(i));
        this.recalculateOccupiedArcs(ia);
        this.recalculateEntries(ia);
    }

    /**
     * Retrieve and store all the entry/leave times for a particular train on a particular schedule. They will be used later to
     * make sure that no trains follows sooner than 5 minutes after another train. See {@link Itinerary#getArrivalTime(Node)}
     * and {@link Itinerary#getLeaveTime(Node)} for details on how these times are calculated.
     * 
     * @param ia The changed schedule for the train.
     */
    private void recalculateEntries(final ItineraryAssignment ia) {
        final Train t = ia.getTrain();
        final Itinerary i = ia.getItinerary();
        this.entries.resetTimes(t);
        for (final Node n : ia.getRoute().getProgression().getNodes()) {
            if (!i.hasNode(n)) {
                continue;
            }
            final long leaveTime = i.getLeaveTime(n);
            if (this.isInPlanningHorizon(leaveTime)) {
                this.entries.setTimes(n, t, i.getArrivalTime(n), i.getLeaveTime(n));
            }
        }
    }

    /**
     * <p>
     * Recalculate the arcs occupied by the train on a particular schedule that will later be used to make sure no trains
     * conflict with each other. Performance of occupancy-related parts of the algorithm is a main factor in the overall
     * performance.
     * </p>
     * 
     * <ul>
     * <li>Calculating occupied arcs for a particular train at a particular time is in itself a time-consuming activity. See
     * {@link Itinerary#getOccupiedArcs(long)} for details.</li>
     * <li>Furthermore, it must be performed for every train.</li>
     * <li>And what's worst, it needs to be calculated many times within the planning horizon, as the trains move on their
     * routes and occupy different arcs every time.</li>
     * </ul>
     * 
     * <p>
     * For these reasons, it is extremely important that the bits calculating occupied arcs be as optimized as possible. With
     * the amount of data we're dealing with here, even too slow iteration or iteration overhead will have a tremendous impact
     * on the algorithm's performance.
     * </p>
     * 
     * <p>
     * We implement some smart measures to reduce the number of cycles necessary for this:
     * </p>
     * 
     * <ul>
     * <li>We only re-calculate the occupied arcs when they've actually changed. That means that if a new {@link WaitTime}
     * appeared in a schedule, we only re-calculate occupied arcs after that particular node. See
     * {@link #getFirstChangeTime(Itinerary)} for details.</li>
     * <li>We make sure that every time train occupies no arcs (when it's not in the territory), we don't even include the train
     * in the calculation. See {@link ConflictRegistry} for details.</li>
     * </ul>
     * 
     * @param ia The changed schedule for the train.
     */
    private void recalculateOccupiedArcs(final ItineraryAssignment ia) {
        /*
         * FIXME If it were possible for a train to have at the same time both entrytime > 0 and origin != depo, this code would
         * miss the arcs occupied by the train before it "magically" appeared in the middle of the territory.
         */
        final int scanEveryXMillis = 60000 / ScoreCalculator.OCCUPATION_CHECKS_PER_MINUTE;
        final long horizon = this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        // insert the number of conflicts for the given assignments
        final Itinerary i = ia.getItinerary();
        final Train t = ia.getTrain();
        final long trainEntryTime = Math.max(0, i.getArrivalTime(i.getTrain().getOrigin()));
        final long startingTime = Math.max(trainEntryTime, this.getFirstChangeTime(i));
        final long endingTime = Math.min(i.getArrivalTime(t.getDestination()), horizon);
        for (long time = 0; time <= horizon; time += scanEveryXMillis) {
            if (time < trainEntryTime || time > endingTime) {
                // clear everywhere the train isn't en route
                this.conflicts.setOccupiedArcs(time, t, OccupationTracker.Builder.empty());
            } else if (time >= startingTime && time <= endingTime) {
                // re-calculate what's actually changed
                this.conflicts.setOccupiedArcs(time, t, i.getOccupiedArcs(time));
            } else {
                // don't touch stuff that doesn't need recalculating
            }
        }
    }

    /**
     * Prepare the calculator for working on a completely different solution. Resets all the caches.
     * 
     * @param workingSolution The solution to be used from now on.
     */
    @Override
    public void resetWorkingSolution(final ProblemSolution workingSolution) {
        this.solution = workingSolution;
        this.wantTimePenalties.clear();
        this.unpreferredTracksPenalties.clear();
        this.scheduleAdherencePenalties.clear();
        this.delayPenalties.clear();
        this.conflicts = new ConflictRegistry(
                (int) this.solution.getPlanningHorizon(TimeUnit.MINUTES)
                        * ScoreCalculator.OCCUPATION_CHECKS_PER_MINUTE + 1);
        this.entries = new EntryRegistry(Node.count());
        for (final ItineraryAssignment ia : this.solution.getAssignments()) {
            ia.getItinerary().resetLatestWaitTimeChange();
            this.modify(ia);
        }
    }

}

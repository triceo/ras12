package org.drools.planner.examples.ras2012;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.core.score.director.incremental.AbstractIncrementalScoreCalculator;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Train;

public class RAS2012ScoreCalculator extends AbstractIncrementalScoreCalculator<RAS2012Solution> {

    private static boolean isInPlanningHorizon(final long time) {
        return time <= RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
    }

    private static int roundMillisecondsToWholeHours(final long milliseconds) {
        final int result = (int) Math.ceil(Math.abs(milliseconds) / 1000.0 / 60.0 / 60.0);
        if (milliseconds < 0) {
            return -result;
        } else {
            return result;
        }
    }

    private RAS2012Solution                              solution                   = null;

    private final Map<Train, Boolean>                    didTrainArrive             = new HashMap<Train, Boolean>();

    private final Map<Train, Integer>                    wantTimePenalties          = new HashMap<Train, Integer>();

    private final Map<Train, Integer>                    delayPenalties             = new HashMap<Train, Integer>();

    private final Map<Train, Integer>                    scheduleAdherencePenalties = new HashMap<Train, Integer>();

    private final Map<Train, Integer>                    unpreferredTracksPenalties = new HashMap<Train, Integer>();

    private HardAndSoftScore                             cache                      = null;

    private final Map<Long, Map<Train, Collection<Arc>>> occupiedArcs               = new HashMap<Long, Map<Train, Collection<Arc>>>();

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
    public synchronized HardAndSoftScore calculateScore() {
        if (this.cache == null) {
            this.cache = this.calculateScoreUncached();
        }
        return this.cache;
    }

    private HardAndSoftScore calculateScoreUncached() {
        int penalty = 0;
        for (final Train t : this.solution.getTrains()) {
            penalty += this.wantTimePenalties.containsKey(t) ? this.wantTimePenalties.get(t) : 0;
            penalty += this.delayPenalties.containsKey(t) ? this.delayPenalties.get(t) : 0;
            penalty += this.scheduleAdherencePenalties.containsKey(t) ? this.scheduleAdherencePenalties
                    .get(t) : 0;
            penalty += this.unpreferredTracksPenalties.containsKey(t) ? this.unpreferredTracksPenalties
                    .get(t) : 0;
        }
        final int conflicts = this.getConflicts();
        if (conflicts > 0) {
            return DefaultHardAndSoftScore.valueOf(-conflicts, -penalty);
        } else {
            return DefaultHardAndSoftScore.valueOf(this.getTrainArrivalMetrics(), -penalty);
        }
    }

    private void clearEveryCache() {
        this.cache = null;
        this.wantTimePenalties.clear();
        this.unpreferredTracksPenalties.clear();
        this.scheduleAdherencePenalties.clear();
        this.delayPenalties.clear();
        this.didTrainArrive.clear();
        this.occupiedArcs.clear();
    }

    private boolean didTrainArrive(final Itinerary producer) {
        final SortedMap<Long, Node> nodes = producer.getSchedule();
        for (final SortedMap.Entry<Long, Node> entry : nodes.entrySet()) {
            if (!RAS2012ScoreCalculator.isInPlanningHorizon(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == producer.getTrain().getDestination()) {
                return true;
            }
        }
        return false;
    }

    private int getConflicts() {
        int conflicts = 0;
        for (final Map<Train, Collection<Arc>> entry : this.occupiedArcs.values()) {
            final Set<Arc> conflictingArcs = new HashSet<Arc>();
            for (final Collection<Arc> arcsOccupiedByTrain : entry.values()) {
                for (final Arc a : arcsOccupiedByTrain) {
                    if (conflictingArcs.contains(a)) {
                        conflicts++;
                    } else {
                        conflictingArcs.add(a);
                    }
                }
            }
        }
        return conflicts;
    }

    private int getDelayPenalty(final Itinerary i, final RAS2012Solution solution) {
        final long delay = i.getDelay();
        final int hoursDelay = RAS2012ScoreCalculator.roundMillisecondsToWholeHours(delay);
        return Math.max(0, hoursDelay) * i.getTrain().getType().getDelayPenalty();
    }

    private int getScheduleAdherencePenalty(final Itinerary i, final RAS2012Solution solution) {
        int penalty = 0;
        if (i.getTrain().getType().adhereToSchedule()) {
            for (final long difference : i.getScheduleAdherenceStatus().values()) {
                if (difference < 1) {
                    continue;
                }
                final int hourlyDifference = RAS2012ScoreCalculator
                        .roundMillisecondsToWholeHours(difference);
                if (hourlyDifference > 2) {
                    penalty += (hourlyDifference - 2) * 200;
                }
            }
        }
        return penalty;
    }

    private int getTrainArrivalMetrics() {
        int num = 0;
        for (final Map.Entry<Train, Boolean> entry : this.didTrainArrive.entrySet()) {
            if (entry.getValue()) {
                final Train t = entry.getKey();
                num += Math.ceil(t.getType().getDelayPenalty() / 100.0);
            }
        }
        return num;
    }

    private int getWantTimePenalty(final Itinerary i, final RAS2012Solution solution) {
        int penalty = 0;
        final Map<Long, Long> wantTimeDifferences = i.getWantTimeDifference();
        for (final Map.Entry<Long, Long> entry : wantTimeDifferences.entrySet()) {
            int hourlyDifference = 0;
            if (!RAS2012ScoreCalculator.isInPlanningHorizon(entry.getKey())) {
                // difference occured past the planning horizon; we don't care about it
                continue;
            }
            final long wantTimeDifference = entry.getValue();
            if (wantTimeDifference > 0) {
                final int hours = RAS2012ScoreCalculator
                        .roundMillisecondsToWholeHours(wantTimeDifference);
                if (hours > 3) {
                    hourlyDifference = hours - 3;
                }
            } else if (wantTimeDifference < 0) {
                final int hours = RAS2012ScoreCalculator
                        .roundMillisecondsToWholeHours(wantTimeDifference);
                if (hours < -1) {
                    hourlyDifference = Math.abs(hours + 1);
                }
            }
            penalty += hourlyDifference * 75;
        }
        return penalty;
    }

    private void insert(final ItineraryAssignment ia) {
        final Train t = ia.getTrain();
        final Itinerary i = ia.getItinerary();
        this.wantTimePenalties.put(t, this.getWantTimePenalty(i, this.solution));
        this.unpreferredTracksPenalties.put(t, RAS2012ScoreCalculator
                .roundMillisecondsToWholeHours(i.getTimeSpentOnUnpreferredTracks(RAS2012Solution
                        .getPlanningHorizon(TimeUnit.MILLISECONDS)) * 50));
        this.scheduleAdherencePenalties.put(t, this.getScheduleAdherencePenalty(i, this.solution));
        this.delayPenalties.put(t, this.getDelayPenalty(i, this.solution));
        this.didTrainArrive.put(t, this.didTrainArrive(i));
        this.recalculateOccupiedArcs(ia);
    }

    private void recalculateOccupiedArcs(final ItineraryAssignment ia) {
        // insert the number of conflicts for the given assignments
        for (long milliseconds = 0; RAS2012ScoreCalculator.isInPlanningHorizon(milliseconds); milliseconds += 30000) {
            if (!this.occupiedArcs.containsKey(milliseconds)) {
                this.occupiedArcs.put(milliseconds, new HashMap<Train, Collection<Arc>>());
            }
            final Map<Train, Collection<Arc>> arcs = this.occupiedArcs.get(milliseconds);
            arcs.put(ia.getTrain(), ia.getItinerary().getCurrentlyOccupiedArcs(milliseconds));
        }
    }

    @Override
    public void resetWorkingSolution(final RAS2012Solution workingSolution) {
        this.clearEveryCache();
        this.solution = workingSolution;
        for (final ItineraryAssignment ia : this.solution.getAssignments()) {
            this.insert(ia);
        }
    }

    private void retract(final ItineraryAssignment ia) {
        final Train t = ia.getTrain();
        this.cache = null;
        this.wantTimePenalties.remove(t);
        this.unpreferredTracksPenalties.remove(t);
        this.scheduleAdherencePenalties.remove(t);
        this.delayPenalties.remove(t);
        this.didTrainArrive.remove(t);
        for (final Map.Entry<Long, Map<Train, Collection<Arc>>> entry : this.occupiedArcs
                .entrySet()) {
            entry.getValue().remove(ia.getTrain());
        }
    }
}

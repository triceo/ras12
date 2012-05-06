package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.util.Converter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ItineraryTest {

    private static Collection<Arc> calculateOccupiedArcsWithKnownPosition(final Itinerary i,
            final Node n) {
        return ItineraryTest.calculateOccupiedArcsWithKnownPosition(i, n, i.getTrain().getLength());
    }

    private static Collection<Arc> calculateOccupiedArcsWithKnownPosition(final Itinerary i,
            final Node n, final BigDecimal remainingLength) {
        final Collection<Arc> results = new LinkedHashSet<Arc>();
        BigDecimal remainingTrainLength = remainingLength;
        Arc arc = i.getRoute().getProgression().getWithDestinationNode(n);
        while (remainingTrainLength.compareTo(BigDecimal.ZERO) > 0) {
            if (arc == null) { // train not yet fully en route
                break;
            }
            results.add(arc);
            remainingTrainLength = remainingTrainLength.subtract(arc.getLengthInMiles());
            arc = i.getRoute().getProgression().getPrevious(arc);
        }
        return results;
    }

    private static Collection<Arc> calculateOccupiedArcsWithUnknownPosition(final Itinerary i,
            final long time) {
        final Collection<Arc> results = new LinkedHashSet<Arc>();
        // find where we are in the leading arc
        final Arc leadingArc = i.getLeadingArc(time);
        if (leadingArc == null) { // journey is over
            return results;
        }
        final BigDecimal distanceTravelledInLeadingArc = Converter.getDistanceTravelledInTheArc(i,
                leadingArc, time);
        if (distanceTravelledInLeadingArc.compareTo(BigDecimal.ZERO) > 0) {
            results.add(leadingArc);
        }
        // and add the rest of the train, if necessary
        final boolean travelledMoreThanTrainLength = distanceTravelledInLeadingArc.compareTo(i
                .getTrain().getLength()) > 0;
        if (!travelledMoreThanTrainLength) {
            final Node lastKnownPoint = leadingArc.getOrigin(i.getRoute());
            final BigDecimal remainingTrainLength = i.getTrain().getLength()
                    .subtract(distanceTravelledInLeadingArc);
            results.addAll(ItineraryTest.calculateOccupiedArcsWithKnownPosition(i, lastKnownPoint,
                    remainingTrainLength));
        }
        return results;
    }

    @Parameters
    public static Collection<Object[]> getInput() {
        final Collection<Object[]> providers = new ArrayList<Object[]>();
        providers.addAll(ItineraryTest.unwrapProvider(new RDS1ItineraryProvider()));
        providers.addAll(ItineraryTest.unwrapProvider(new RDS2ItineraryProvider()));
        providers.addAll(ItineraryTest.unwrapProvider(new RDS3ItineraryProvider()));
        return providers;
    }

    private static Collection<Object[]> unwrapProvider(final ItineraryProvider provider) {
        final List<Object[]> itineraries = new LinkedList<Object[]>();
        for (final Itinerary i : provider.getItineraries()) {
            itineraries.add(new Object[] { i });
        }
        return itineraries;
    }

    private final Itinerary i;

    public ItineraryTest(final Itinerary i) {
        this.i = i;
    }

    @Test
    public void testGetCurrentlyOccupiedArcs() {
        for (long time = 0; time <= RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS); time += 1000) {
            if (time <= this.i.getTrain().getEntryTime(TimeUnit.MILLISECONDS)) {
                if (this.i.getTrain().getOrigin() == this.i.getRoute().getProgression().getOrigin()
                        .getOrigin(this.i.getRoute())) {
                    // the train shouldn't be en route yet
                    Assert.assertEquals("No occupied arcs for " + this.i + " at " + time
                            + " (before entry time)", Collections.EMPTY_SET,
                            this.i.getCurrentlyOccupiedArcs(time));
                } else {
                    // train starts somewhere in the middle of the route
                    Assert.assertEquals("Occupied arcs for " + this.i + " at " + time
                            + " (before entry time, different origin)", ItineraryTest
                            .calculateOccupiedArcsWithKnownPosition(this.i, this.i.getTrain()
                                    .getOrigin()), this.i.getCurrentlyOccupiedArcs(time));
                }
                continue;
            }
            Assert.assertEquals("Occupied arcs for " + this.i + " at " + time,
                    ItineraryTest.calculateOccupiedArcsWithUnknownPosition(this.i, time),
                    this.i.getCurrentlyOccupiedArcs(time));
        }
    }

    @Test
    public void testGetDelayAtHorizon() {
        final long timeInterestedIn = RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        final SortedMap<Long, Node> schedulePast = this.i.getSchedule().headMap(
                timeInterestedIn + 1);
        if (schedulePast.values().contains(this.i.getTrain().getDestination())) {
            // train did finish in time, we don't need to estimate
            final long actualTimeOfArrival = this.i.getSchedule().lastKey();
            final long wantTime = this.i.getTrain().getWantTime(TimeUnit.MILLISECONDS);
            final long delay = actualTimeOfArrival - wantTime;
            Assert.assertEquals("Exact delay for " + this.i, delay, this.i.getDelay());
        } else {
            // train didn't finish in time, we need to estimate
            final BigDecimal actualDistanceTravelled = Converter.calculateActualDistanceTravelled(
                    this.i, timeInterestedIn);
            final long travellingTime = Converter.getTimeFromSpeedAndDistance(this.i.getTrain()
                    .getMaximumSpeed(), actualDistanceTravelled);
            final long totalTravellingTime = travellingTime
                    + this.i.getTrain().getEntryTime(TimeUnit.MILLISECONDS);
            Assert.assertEquals("Estimated delay for " + this.i, timeInterestedIn
                    - totalTravellingTime, this.i.getDelay());
        }
    }

    /**
     * Technically we shouldn't be testing getLeadingArc() as it's not a public API. However, since this method is absolutely
     * crucial to the workings of Itinerary, we make an exception here.
     */
    @Test
    public void testGetLeadingArc() {
        // assemble a list of "checkpoint" where the train should be at which times
        final Map<Long, Arc> expecteds = new HashMap<Long, Arc>();
        final Route r = this.i.getRoute();
        final Train t = this.i.getTrain();
        long totalTime = t.getEntryTime(TimeUnit.MILLISECONDS);
        Arc currentArc = null;
        if (totalTime > 0) {
            // the train shouldn't be on the route before its time of entry
            expecteds.put((long) 0, null);
            expecteds.put(totalTime / 2, null);
        }
        while ((currentArc = r.getProgression().getNext(currentArc)) != null) {
            // account for possible maintenance windows
            final Node n = currentArc.getOrigin(r);
            if (!this.i.isNodeOnRoute(n)) { // sometimes a train doesn't start at the beginning of a route
                continue;
            }
            if (this.i.getMaintenances().containsKey(n)
                    && this.i.getMaintenances().get(n).isInside(totalTime, TimeUnit.MILLISECONDS)) {
                totalTime = this.i.getMaintenances().get(n).getEnd(TimeUnit.MILLISECONDS);
            }
            expecteds.put(totalTime, currentArc); // immediately after entering the node
            final long arcTravellingTime = t
                    .getArcTravellingTime(currentArc, TimeUnit.MILLISECONDS);
            final long arcTravellingTimeThird = arcTravellingTime / 3;
            expecteds.put(totalTime + arcTravellingTimeThird, currentArc); // one third into the node
            totalTime += arcTravellingTime;
            expecteds.put(totalTime - arcTravellingTimeThird, currentArc); // two thirds into the node
        }
        // and now validate against reality
        for (final Map.Entry<Long, Arc> entry : expecteds.entrySet()) {
            if (entry.getKey() > RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS)) {
                // don't measure beyond the planning horizon
                break;
            }
            Assert.assertEquals("Train " + t.getName() + " on route " + r.getId() + " at time "
                    + entry.getKey() + " isn't where it's supposed to be.", entry.getValue(),
                    this.i.getLeadingArc(entry.getKey()));
        }
    }

    @Ignore
    @Test
    public void testGetSchedule() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Ignore
    @Test
    public void testWaitTimes() {
        Assert.fail("Not yet implemented"); // TODO
    }

}

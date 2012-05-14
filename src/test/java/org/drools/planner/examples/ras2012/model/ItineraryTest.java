package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.util.Converter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ItineraryTest extends AbstractItineraryProviderBasedTest {

    private static Collection<Arc> calculateOccupiedArcsAfterArrival(final Itinerary i,
            final long time) {
        final long arrivalTime = i.getSchedule().lastKey();
        if (time <= arrivalTime) {
            throw new IllegalArgumentException(
                    "Cannot call this method when the train didn't yet finish.");
        }
        final Train t = i.getTrain();
        final BigDecimal distanceTravelled = Converter.getDistanceInMilesFromSpeedAndTime(
                t.getMaximumSpeed(Track.MAIN_0), time - arrivalTime);
        final BigDecimal remainingTrainLength = t.getLengthInMiles().subtract(distanceTravelled);
        if (remainingTrainLength.signum() <= 0) {
            return Collections.emptySet();
        }
        return ItineraryTest.calculateOccupiedArcsWithKnownPosition(i, i.getTrain()
                .getDestination(), remainingTrainLength);
    }

    private static Collection<Arc> calculateOccupiedArcsWithKnownPosition(final Itinerary i,
            final Node n) {
        return ItineraryTest.calculateOccupiedArcsWithKnownPosition(i, n, i.getTrain()
                .getLengthInMiles());
    }

    private static Collection<Arc> calculateOccupiedArcsWithKnownPosition(final Itinerary i,
            final Node n, final BigDecimal remainingLength) {
        final Collection<Arc> results = new LinkedHashSet<Arc>();
        BigDecimal remainingTrainLength = remainingLength;
        Arc arc = i.getRoute().getProgression().getWithDestinationNode(n);
        while (remainingTrainLength.signum() > 0) {
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
        if (distanceTravelledInLeadingArc.signum() > 0) {
            results.add(leadingArc);
        }
        // and add the rest of the train, if necessary
        final boolean travelledMoreThanTrainLength = distanceTravelledInLeadingArc.compareTo(i
                .getTrain().getLengthInMiles()) > 0;
        if (!travelledMoreThanTrainLength) {
            final Node lastKnownPoint = leadingArc.getOrigin(i.getRoute());
            final BigDecimal remainingTrainLength = i.getTrain().getLengthInMiles()
                    .subtract(distanceTravelledInLeadingArc);
            results.addAll(ItineraryTest.calculateOccupiedArcsWithKnownPosition(i, lastKnownPoint,
                    remainingTrainLength));
        }
        return results;
    }

    @Parameters
    public static Collection<Object[]> getInput() {
        final Collection<Object[]> providers = new ArrayList<Object[]>();
        for (final ItineraryProvider provider : AbstractItineraryProviderBasedTest.getProviders()) {
            providers.addAll(ItineraryTest.unwrapProvider(provider));
        }
        return providers;
    }

    private static Collection<Object[]> unwrapProvider(final ItineraryProvider provider) {
        final List<Object[]> itineraries = new LinkedList<Object[]>();
        for (final Itinerary i : provider.getItineraries()) {
            itineraries.add(new Object[] { i, provider.getSolution() });
        }
        return itineraries;
    }

    private final RAS2012Solution solution;

    private final Itinerary       itinerary;

    public ItineraryTest(final Itinerary i, final RAS2012Solution solution) {
        this.itinerary = i;
        this.solution = solution;
    }

    @Test
    public void testGetDelay() {
        final long timeInterestedIn = this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        final SortedMap<Long, Node> schedulePast = this.itinerary.getSchedule().headMap(
                timeInterestedIn + 1);
        if (schedulePast.values().contains(this.itinerary.getTrain().getDestination())) {
            // train did finish in time, we don't need to estimate
            final long actualTimeOfArrival = this.itinerary.getSchedule().lastKey();
            final long wantTime = this.itinerary.getTrain().getWantTime(TimeUnit.MILLISECONDS);
            final long delay = actualTimeOfArrival - wantTime;
            Assert.assertEquals("Exact delay for " + this.itinerary, delay,
                    this.itinerary.getDelay(timeInterestedIn));
        } else {
            // train didn't finish in time, we need to estimate
            final BigDecimal actualDistanceTravelled = Converter.calculateActualDistanceTravelled(
                    this.itinerary, timeInterestedIn);
            final long travellingTime = Converter.getTimeFromSpeedAndDistance(this.itinerary
                    .getTrain().getMaximumSpeed(), actualDistanceTravelled);
            final long totalTravellingTime = travellingTime
                    + this.itinerary.getTrain().getEntryTime(TimeUnit.MILLISECONDS);
            Assert.assertEquals("Estimated delay for " + this.itinerary, timeInterestedIn
                    - totalTravellingTime, this.itinerary.getDelay(timeInterestedIn));
        }
    }

    /**
     * Technically we shouldn't be testing getLeadingArc() as it's not a public API. However, since this method is absolutely
     * crucial to the workings of Itinerary, we make an exception here.
     */
    @Test
    public void testGetLeadingArc() {
        // assemble a list of "checkpoint" where the train should be at which times
        final SortedMap<Long, Arc> expecteds = new TreeMap<Long, Arc>();
        final Route r = this.itinerary.getRoute();
        final Train t = this.itinerary.getTrain();
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
            if (!this.itinerary.isNodeOnRoute(n)) { // sometimes a train doesn't start at the beginning of a route
                continue;
            }
            if (this.itinerary.getMaintenances().containsKey(n)
                    && this.itinerary.getMaintenances().get(n)
                            .isInside(totalTime, TimeUnit.MILLISECONDS)) {
                totalTime = this.itinerary.getMaintenances().get(n).getEnd(TimeUnit.MILLISECONDS);
            }
            expecteds.put(totalTime + 1, currentArc); // immediately after entering the node
            final long arcTravellingTime = t
                    .getArcTravellingTime(currentArc, TimeUnit.MILLISECONDS);
            final long arcTravellingTimeThird = arcTravellingTime / 3;
            expecteds.put(totalTime + arcTravellingTimeThird, currentArc); // one third into the node
            totalTime += arcTravellingTime;
            expecteds.put(totalTime - arcTravellingTimeThird, currentArc); // two thirds into the node
            expecteds.put(totalTime, currentArc); // at the millisecond of entering the node, we shouldn't yet be in the
                                                  // next arc
        }
        // and now validate against reality
        for (final Map.Entry<Long, Arc> entry : expecteds.entrySet()) {
            if (entry.getKey() > this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS)) {
                // don't measure beyond the planning horizon
                break;
            }
            Assert.assertEquals("Train " + t.getName() + " on route " + r.getId() + " at time "
                    + entry.getKey() + " isn't where it's supposed to be.", entry.getValue(),
                    this.itinerary.getLeadingArc(entry.getKey()));
        }
    }

    // FIXME this method should not only compare the arcs, but also the total mileage
    @Test
    public void testGetOccupiedArcs() {
        final Train t = this.itinerary.getTrain();
        final Route r = this.itinerary.getRoute();
        final long arrivalTime = this.itinerary.getSchedule().lastKey();
        for (long time = 0; time < this.solution.getPlanningHorizon(TimeUnit.MILLISECONDS); time += 1000) {
            final Collection<Arc> occupiedArcs = this.itinerary.getOccupiedArcs(time)
                    .getIncludedArcs();
            if (time <= t.getEntryTime(TimeUnit.MILLISECONDS)) {
                if (t.getOrigin() == r.getProgression().getOrigin().getOrigin(r)) {
                    // the train shouldn't be en route yet
                    Assert.assertEquals("No occupied arcs for " + this.itinerary + " at " + time
                            + " (entry time " + t.getEntryTime(TimeUnit.MILLISECONDS) + ")",
                            Collections.emptySet(), occupiedArcs);
                } else {
                    // train starts somewhere in the middle of the route
                    Assert.assertEquals(
                            "Occupied arcs for " + this.itinerary + " at " + time + " (entry time "
                                    + t.getEntryTime(TimeUnit.MILLISECONDS) + ", origin "
                                    + t.getOrigin() + ")",
                            ItineraryTest.calculateOccupiedArcsWithKnownPosition(this.itinerary,
                                    t.getOrigin()), occupiedArcs);
                }
            } else if (time > arrivalTime) {
                // train should be gradually leaving the network
                Assert.assertEquals("Occupied arcs for " + this.itinerary + " at " + time
                        + " (arrival time " + arrivalTime + ")",
                        ItineraryTest.calculateOccupiedArcsAfterArrival(this.itinerary, time),
                        occupiedArcs);
            } else {
                // business as usual
                Assert.assertEquals("Occupied arcs for " + this.itinerary + " at " + time,
                        ItineraryTest
                                .calculateOccupiedArcsWithUnknownPosition(this.itinerary, time),
                        occupiedArcs);
            }
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

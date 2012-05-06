package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Train;
import org.drools.planner.examples.ras2012.model.original.Train.Type;
import org.drools.planner.examples.ras2012.util.Converter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractItineraryTest {

    public static Collection<Arc> calculateOccupiedArcsWithKnownPosition(final Itinerary i,
            final Node n) {
        return AbstractItineraryTest.calculateOccupiedArcsWithKnownPosition(i, n, i.getTrain()
                .getLength());
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

    public static Collection<Arc> calculateOccupiedArcsWithUnknownPosition(final Itinerary i,
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
            results.addAll(AbstractItineraryTest.calculateOccupiedArcsWithKnownPosition(i,
                    lastKnownPoint, remainingTrainLength));
        }
        return results;
    }

    private RAS2012Solution        solution;

    private Map<Train, Set<Route>> testedRoutes;

    /**
     * Return the solution to get the itineraries from.
     * 
     * @return Solution as a result of parsing a data set.
     */
    protected abstract RAS2012Solution fetchSolution();

    /**
     * Generate a list of trains and routes that the itinerary should be tested on.
     * 
     * @return
     */
    private Map<Train, Set<Route>> fetchTestedRoutes() {
        final Map<Train, Set<Route>> results = new TreeMap<Train, Set<Route>>();
        final RAS2012Solution sol = this.getSolution();
        Type lastUsedWestboundTrainType = null;
        Type lastUsedEastboundTrainType = null;
        final Collection<Route> routes = new LinkedHashSet<Route>(sol.getNetwork().getAllRoutes());
        for (final Train t : sol.getTrains()) {
            /*
             * pick only one train for every train type and direction; depends on the assumption that the trains are sorted by
             * their names, thus AX trains will always come after BX trains
             */
            final Type lastUsedTrainType = t.isEastbound() ? lastUsedEastboundTrainType
                    : lastUsedWestboundTrainType;
            if (t.getType() == lastUsedTrainType) {
                continue;
            }
            // take only the possible routes that are yet unused
            final SortedSet<Route> routeSet = new TreeSet<Route>();
            for (final Route r : sol.getNetwork().getRoutes(t)) {
                if (routes.contains(r)) {
                    routeSet.add(r);
                }
            }
            // and pick the best and worst of those
            final SortedSet<Route> result = new TreeSet<Route>();
            result.add(routeSet.first());
            result.add(routeSet.last());
            results.put(t, result);
            routes.removeAll(result);
            if (t.isEastbound()) {
                lastUsedEastboundTrainType = t.getType();
            } else {
                lastUsedWestboundTrainType = t.getType();
            }
        }
        return results;
    }

    /**
     * Allows the test to specify which trains and which routes it can handle.
     * 
     * @return Keys are names of trains, values are arrays of route numbers that these trains will be travelling on.
     */
    protected abstract Map<String, int[]> getExpectedValues();

    protected abstract List<Itinerary> getItineraries();

    protected Itinerary getItinerary(final String trainName, final int routeId) {
        for (final Map.Entry<Train, Set<Route>> entry : this.getTestedRoutes().entrySet()) {
            if (!entry.getKey().getName().equals(trainName)) {
                continue;
            }
            for (final Route r : entry.getValue()) {
                if (r.getId() == routeId) {
                    return this.getItinerary(entry.getKey(), r);
                }
            }

        }
        throw new IllegalArgumentException("Itinerary for train " + trainName + ", route "
                + routeId + " not found.");
    }

    protected Itinerary getItinerary(final Train t, final Route r) {
        final Itinerary i = new Itinerary(r, t, this.getSolution().getMaintenances());
        final File f = new File(this.getTargetDataFolder(), "route" + r.getId() + "_train"
                + t.getName() + ".png");
        if (!f.exists()) {
            i.visualize(f);
        }
        return i;
    }

    protected synchronized RAS2012Solution getSolution() {
        if (this.solution == null) {
            this.solution = this.fetchSolution();
        }
        return this.solution;
    }

    protected File getTargetDataFolder() {
        return new File("data/tests/", this.getClass().getName());
    }

    /**
     * Return the list of values that this itinerary should be tested on, and run them through validation before that.
     * 
     * @return
     */
    protected synchronized Map<Train, Set<Route>> getTestedRoutes() {
        if (this.testedRoutes == null) {
            final Map<Train, Set<Route>> toTest = this.fetchTestedRoutes();
            final Map<String, int[]> expected = this.getExpectedValues();
            // make sure we have all the expected trains
            final Set<Train> trains = toTest.keySet();
            Assert.assertEquals("There must only be the expected trains.", expected.size(),
                    trains.size());
            for (final String trainName : expected.keySet()) {
                boolean found = false;
                for (final Train t : trains) {
                    if (t.getName().equals(trainName)) {
                        found = true;
                        break;
                    }
                }
                Assert.assertTrue("Train name missing: " + trainName, found);
            }
            // make sure we have all the expected routes
            for (final Train t : trains) {
                final String name = t.getName();
                final List<Integer> expectedRouteIds = new LinkedList<Integer>();
                for (final int id : expected.get(name)) {
                    expectedRouteIds.add(id);
                }
                Assert.assertEquals("There must only be the expected routes.",
                        expectedRouteIds.size(), toTest.get(t).size());
                for (final Route r : toTest.get(t)) {
                    if (!expectedRouteIds.contains(r.getId())) {
                        // Assert.fail("Train " + t.getName() + " shouldn't have route " + r.getId());
                    }
                }
            }
            this.testedRoutes = toTest;
            // now write everything into data files for future reference
            final File folder = this.getTargetDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            this.getSolution().getNetwork().visualize(new File(folder, "network.png"));
            for (final Set<Route> routes : this.testedRoutes.values()) {
                for (final Route route : routes) {
                    final File f = new File(folder, "route" + route.getId() + ".png");
                    if (!f.exists()) {
                        route.visualize(f);
                    }
                }
            }
        }
        return this.testedRoutes;
    }

    @Test
    public void testGetCurrentlyOccupiedArcs() {
        for (long time = 0; time <= RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS); time += 1000) {
            for (final Itinerary i : this.getItineraries()) {
                if (time <= i.getTrain().getEntryTime(TimeUnit.MILLISECONDS)) {
                    if (i.getTrain().getOrigin() == i.getRoute().getProgression().getOrigin()
                            .getOrigin(i.getRoute())) {
                        // the train shouldn't be en route yet
                        Assert.assertEquals("No occupied arcs for " + i + " at " + time
                                + " (before entry time)", Collections.EMPTY_SET,
                                i.getCurrentlyOccupiedArcs(time));
                    } else {
                        // train starts somewhere in the middle of the route
                        Assert.assertEquals("Occupied arcs for " + i + " at " + time
                                + " (before entry time, different origin)",
                                AbstractItineraryTest.calculateOccupiedArcsWithKnownPosition(i, i
                                        .getTrain().getOrigin()), i.getCurrentlyOccupiedArcs(time));
                    }
                    continue;
                }
                Assert.assertEquals("Occupied arcs for " + i + " at " + time,
                        AbstractItineraryTest.calculateOccupiedArcsWithUnknownPosition(i, time),
                        i.getCurrentlyOccupiedArcs(time));
            }
        }
    }

    @Test
    public void testGetDelayAtHorizon() {
        final long timeInterestedIn = RAS2012Solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        for (final Itinerary i : this.getItineraries()) {
            final SortedMap<Long, Node> schedulePast = i.getSchedule()
                    .headMap(timeInterestedIn + 1);
            if (schedulePast.values().contains(i.getTrain().getDestination())) {
                // train did finish in time, we don't need to estimate
                final long actualTimeOfArrival = i.getSchedule().lastKey();
                final long wantTime = i.getTrain().getWantTime(TimeUnit.MILLISECONDS);
                final long delay = actualTimeOfArrival - wantTime;
                Assert.assertEquals("Exact delay for " + i, delay, i.getDelay());
            } else {
                // train didn't finish in time, we need to estimate
                final BigDecimal actualDistanceTravelled = Converter
                        .calculateActualDistanceTravelled(i, timeInterestedIn);
                final long travellingTime = Converter.getTimeFromSpeedAndDistance(i.getTrain()
                        .getMaximumSpeed(), actualDistanceTravelled);
                final long totalTravellingTime = travellingTime
                        + i.getTrain().getEntryTime(TimeUnit.MILLISECONDS);
                Assert.assertEquals("Estimated delay for " + i, timeInterestedIn
                        - totalTravellingTime, i.getDelay());
            }
        }
    }

    /**
     * Technically we shouldn't be testing getLeadingArc() as it's not a public API. However, since this method is absolutely
     * crucial to the workings of Itinerary, we make an exception here.
     */
    @Test
    public void testGetLeadingArc() {
        for (final Itinerary i : this.getItineraries()) {

            // assemble a list of "checkpoint" where the train should be at which times
            final Map<Long, Arc> expecteds = new HashMap<Long, Arc>();
            final Route r = i.getRoute();
            final Train t = i.getTrain();
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
                if (!i.isNodeOnRoute(n)) { // sometimes a train doesn't start at the beginning of a route
                    continue;
                }
                if (i.getMaintenances().containsKey(n)
                        && i.getMaintenances().get(n).isInside(totalTime, TimeUnit.MILLISECONDS)) {
                    totalTime = i.getMaintenances().get(n).getEnd(TimeUnit.MILLISECONDS);
                }
                expecteds.put(totalTime, currentArc); // immediately after entering the node
                final long arcTravellingTime = t.getArcTravellingTime(currentArc,
                        TimeUnit.MILLISECONDS);
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
                        i.getLeadingArc(entry.getKey()));
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

package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.Train.TrainType;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractItineraryTest {

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
        TrainType lastUsedWestboundTrainType = null;
        TrainType lastUsedEastboundTrainType = null;
        final Collection<Route> westboundRoutes = sol.getNetwork().getAllWestboundRoutes();
        final Collection<Route> eastboundRoutes = sol.getNetwork().getAllEastboundRoutes();
        for (final Train t : sol.getTrains()) {
            /*
             * pick only one train for every train type and direction; depends on the assumption that the trains are sorted by
             * their names, thus AX trains will always come after BX trains
             */
            final TrainType lastUsedTrainType = t.isEastbound() ? lastUsedEastboundTrainType
                    : lastUsedWestboundTrainType;
            if (t.getType() == lastUsedTrainType) {
                continue;
            }
            // get all available routes for the train
            final Collection<Route> routes = t.isEastbound() ? eastboundRoutes : westboundRoutes;
            // take only the equivalent ones
            final SortedSet<Route> routeSet = new TreeSet<Route>(routes);
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

    protected abstract Map<Itinerary, Integer> getHaltInformation();

    protected Itinerary getItinerary(final String trainName, final int routeId) {
        for (final Map.Entry<Train, Set<Route>> entry : this.getTestedRoutes().entrySet()) {
            if (entry.getKey().getName().equals(trainName)) {
                for (final Route r : entry.getValue()) {
                    if (r.getId() == routeId) {
                        return this.getItinerary(entry.getKey(), r);
                    }
                }
            }
        }
        return null;
    }

    protected Itinerary getItinerary(final Train t, final Route r) {
        return new Itinerary(r, t, this.getSolution().getMaintenances());
    }

    protected synchronized RAS2012Solution getSolution() {
        if (this.solution == null) {
            this.solution = this.fetchSolution();
        }
        return this.solution;
    }

    protected File getTargetDataFolder() {
        return new File("data/", this.getClass().getName());
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
                        Assert.fail("Train " + t.getName() + " shouldn't have route " + r.getId());
                    }
                }
            }
            this.testedRoutes = toTest;
            // now write everything into data files for future reference
            final File folder = this.getTargetDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            for (final Train t : trains) {
                for (final Route r : toTest.get(t)) {
                    try {
                        r.toCSV(new FileOutputStream(new File(folder, "Route" + r.getId() + ".csv")));
                    } catch (final FileNotFoundException e) {
                        // ignore
                    }
                }
            }
        }
        return this.testedRoutes;
    }

    @Test
    public void testCountHalts() {
        for (final Map.Entry<Itinerary, Integer> values : this.getHaltInformation().entrySet()) {
            final Itinerary i = values.getKey();
            final int expectedValue = values.getValue();
            Assert.assertEquals("Train " + i.getTrain().getName() + " on route "
                    + i.getRoute().getId() + " has been halted invalid amount of times.",
                    expectedValue, i.countHalts());
        }

    }

    @Test
    public void testGetCurrentArc() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetCurrentlyOccupiedArcs() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetDistanceTravelled() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetNextNodeToReach() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testGetSchedule() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testRemoveWaitTime() {
        Assert.fail("Not yet implemented"); // TODO
    }

    @Test
    public void testSetWaitTime() {
        Assert.fail("Not yet implemented"); // TODO
    }

}

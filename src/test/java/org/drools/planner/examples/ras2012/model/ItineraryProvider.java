package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.Train.Type;
import org.junit.Assert;

public abstract class ItineraryProvider {

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

    public abstract List<Itinerary> getItineraries();

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

    private Itinerary getItinerary(final Train t, final Route r) {
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

    public File getTargetDataFolder() {
        return new File("data/tests/", this.getClass().getName());
    }

    /**
     * Return the list of values that this itinerary should be tested on, and run them through validation before that.
     * 
     * @return
     */
    private synchronized Map<Train, Set<Route>> getTestedRoutes() {
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
                Assert.assertEquals(
                        "There must only be the expected routes for train " + t.getName() + ".",
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

}

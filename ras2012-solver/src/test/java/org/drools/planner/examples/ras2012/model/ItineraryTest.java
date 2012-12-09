package org.drools.planner.examples.ras2012.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.drools.planner.examples.ras2012.ProblemSolution;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ItineraryTest extends AbstractItineraryProviderBasedTest {

    @Parameters
    public static Collection<Object[]> getInput() {
        final Collection<Object[]> providers = new ArrayList<>();
        for (final ItineraryProvider provider : AbstractItineraryProviderBasedTest.getProviders()) {
            providers.addAll(ItineraryTest.unwrapProvider(provider));
        }
        return providers;
    }

    private static Collection<Object[]> unwrapProvider(final ItineraryProvider provider) {
        final List<Object[]> itineraries = new LinkedList<>();
        for (final Itinerary i : provider.getItineraries()) {
            itineraries.add(new Object[] { i, provider.getSolution() });
        }
        return itineraries;
    }

    private final ProblemSolution solution;

    private final Itinerary       itinerary;

    public ItineraryTest(final Itinerary i, final ProblemSolution solution) {
        this.itinerary = i;
        this.solution = solution;
    }

    /**
     * Technically we shouldn't be testing getLeadingArc() as it's not a public API. However, since this method is absolutely
     * crucial to the workings of Itinerary, we make an exception here.
     */
    @Test
    public void testGetLeadingArc() {
        // assemble a list of "checkpoint" where the train should be at which times
        final SortedMap<Long, Arc> expecteds = new TreeMap<>();
        final Route r = this.itinerary.getRoute();
        final Train t = this.itinerary.getTrain();
        long totalTime = t.getEntryTime(TimeUnit.MILLISECONDS);
        Arc currentArc = null;
        if (totalTime > 0) {
            // the train shouldn't be on the route before its time of entry
            expecteds.put((long) 0, null);
            expecteds.put(totalTime / 2, null);
        }
        while ((currentArc = r.getProgression().getNextArc(currentArc)) != null) {
            // account for possible maintenance windows
            final Node n = currentArc.getOrigin(r);
            if (!this.itinerary.hasNode(n)) { // sometimes a train doesn't start at the beginning of a route
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ItineraryTest [problem=").append(this.solution.getName())
                .append(", itinerary=").append(this.itinerary).append("]");
        return builder.toString();
    }

}

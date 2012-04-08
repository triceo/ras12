package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.RAS2012ProblemIO;
import org.drools.planner.examples.ras2012.RAS2012Solution;

public class Itinerary1Test extends AbstractItineraryTest {

    private static final String TRAIN1        = "F1";
    private static final String TRAIN2        = "E1";
    private static final String TRAIN3        = "D3";
    private static final String TRAIN4        = "C2";
    private static final String TRAIN5        = "B3";
    private static final String TRAIN6        = "B1";
    private static final String TRAIN7        = "A2";

    private static final int    TRAIN1_ROUTE0 = 2519;
    private static final int    TRAIN1_ROUTE1 = 4705;
    private static final int    TRAIN2_ROUTE0 = 50;
    private static final int    TRAIN2_ROUTE1 = 2446;
    private static final int    TRAIN3_ROUTE0 = 2612;
    private static final int    TRAIN3_ROUTE1 = 4128;
    private static final int    TRAIN4_ROUTE0 = 198;
    private static final int    TRAIN4_ROUTE1 = 2468;
    private static final int    TRAIN5_ROUTE0 = 72;
    private static final int    TRAIN5_ROUTE1 = 2298;
    private static final int    TRAIN6_ROUTE0 = 3096;
    private static final int    TRAIN6_ROUTE1 = 4798;
    private static final int    TRAIN7_ROUTE0 = 220;
    private static final int    TRAIN7_ROUTE1 = 2320;

    @Override
    protected RAS2012Solution fetchSolution() {
        return new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RDS1.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(Itinerary1Test.TRAIN1, new int[] { Itinerary1Test.TRAIN1_ROUTE0,
                Itinerary1Test.TRAIN1_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN2, new int[] { Itinerary1Test.TRAIN2_ROUTE0,
                Itinerary1Test.TRAIN2_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN3, new int[] { Itinerary1Test.TRAIN3_ROUTE0,
                Itinerary1Test.TRAIN3_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN4, new int[] { Itinerary1Test.TRAIN4_ROUTE0,
                Itinerary1Test.TRAIN4_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN5, new int[] { Itinerary1Test.TRAIN5_ROUTE0,
                Itinerary1Test.TRAIN5_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN6, new int[] { Itinerary1Test.TRAIN6_ROUTE0,
                Itinerary1Test.TRAIN6_ROUTE1 });
        expected.put(Itinerary1Test.TRAIN7, new int[] { Itinerary1Test.TRAIN7_ROUTE0,
                Itinerary1Test.TRAIN7_ROUTE1 });
        return expected;
    }

    @Override
    protected List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<Itinerary>();
        results.add(this.getItinerary(Itinerary1Test.TRAIN1, Itinerary1Test.TRAIN1_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN1, Itinerary1Test.TRAIN1_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN2, Itinerary1Test.TRAIN2_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN2, Itinerary1Test.TRAIN2_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN3, Itinerary1Test.TRAIN3_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN3, Itinerary1Test.TRAIN3_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN4, Itinerary1Test.TRAIN4_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN4, Itinerary1Test.TRAIN4_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN5, Itinerary1Test.TRAIN5_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN5, Itinerary1Test.TRAIN5_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN6, Itinerary1Test.TRAIN6_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN6, Itinerary1Test.TRAIN6_ROUTE1));
        results.add(this.getItinerary(Itinerary1Test.TRAIN7, Itinerary1Test.TRAIN7_ROUTE0));
        results.add(this.getItinerary(Itinerary1Test.TRAIN7, Itinerary1Test.TRAIN7_ROUTE1));
        return results;
    }

}

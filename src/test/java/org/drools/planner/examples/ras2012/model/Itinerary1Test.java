package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
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

    private static final int    TRAIN1_ROUTE0 = 2519; // 1C
    private static final int    TRAIN1_ROUTE1 = 4128; // 10SW, 3C, 5S
    private static final int    TRAIN2_ROUTE0 = 100; // 1C
    private static final int    TRAIN2_ROUTE1 = 2446; // 10SW, 3C, 5S
    private static final int    TRAIN3_ROUTE0 = 2804; // 1C
    private static final int    TRAIN3_ROUTE1 = 4221; // 10SW, 1C, 5S
    private static final int    TRAIN4_ROUTE0 = 50;  // 1C
    private static final int    TRAIN4_ROUTE1 = 2298; // 10SW, 1C, 5S
    private static final int    TRAIN5_ROUTE0 = 248; // 3C
    private static final int    TRAIN5_ROUTE1 = 1834; // 8SW, 3C, 4S
    private static final int    TRAIN6_ROUTE0 = 3096; // 3C
    private static final int    TRAIN6_ROUTE1 = 4112; // 8SW, 3C, 4S
    private static final int    TRAIN7_ROUTE0 = 72;  // 3C
    private static final int    TRAIN7_ROUTE1 = 2222; // 8SW, 3C, 4S

    @Override
    protected RAS2012Solution fetchSolution() {
        return new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RAS DATA SET 1.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(TRAIN1, new int[] { TRAIN1_ROUTE0, TRAIN1_ROUTE1 });
        expected.put(TRAIN2, new int[] { TRAIN2_ROUTE0, TRAIN2_ROUTE1 });
        expected.put(TRAIN3, new int[] { TRAIN3_ROUTE0, TRAIN3_ROUTE1 });
        expected.put(TRAIN4, new int[] { TRAIN4_ROUTE0, TRAIN4_ROUTE1 });
        expected.put(TRAIN5, new int[] { TRAIN5_ROUTE0, TRAIN5_ROUTE1 });
        expected.put(TRAIN6, new int[] { TRAIN6_ROUTE0, TRAIN6_ROUTE1 });
        expected.put(TRAIN7, new int[] { TRAIN7_ROUTE0, TRAIN7_ROUTE1 });
        return expected;
    }

    @Override
    protected Map<Itinerary, Integer> getHaltInformation() {
        Map<Itinerary, Integer> results = new HashMap<Itinerary, Integer>();
        results.put(this.getItinerary(TRAIN1, TRAIN1_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN1, TRAIN1_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN2, TRAIN2_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN2, TRAIN2_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN3, TRAIN3_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN3, TRAIN3_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN4, TRAIN4_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN4, TRAIN4_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN5, TRAIN5_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN5, TRAIN5_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN6, TRAIN6_ROUTE0), 1);
        results.put(this.getItinerary(TRAIN6, TRAIN6_ROUTE1), 0);
        results.put(this.getItinerary(TRAIN7, TRAIN7_ROUTE0), 0);
        results.put(this.getItinerary(TRAIN7, TRAIN7_ROUTE1), 1);
        return results;

    }

}

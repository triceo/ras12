package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.RAS2012ProblemIO;
import org.drools.planner.examples.ras2012.RAS2012Solution;

public class Itinerary3Test extends AbstractItineraryTest {

    private static final String TRAIN1         = "A1";
    private static final String TRAIN2         = "A2";
    private static final String TRAIN3         = "B1";
    private static final String TRAIN4         = "B2";
    private static final String TRAIN5         = "C1";
    private static final String TRAIN6         = "C2";
    private static final String TRAIN7         = "D1";
    private static final String TRAIN8         = "E1";
    private static final String TRAIN9         = "E2";
    private static final String TRAIN10        = "F1";
    private static final String TRAIN11        = "F2";

    private static final int    TRAIN1_ROUTE0  = 50;
    private static final int    TRAIN1_ROUTE1  = 2446;
    private static final int    TRAIN2_ROUTE0  = 2519;
    private static final int    TRAIN2_ROUTE1  = 4705;
    private static final int    TRAIN3_ROUTE0  = 2612;
    private static final int    TRAIN3_ROUTE1  = 4128;
    private static final int    TRAIN4_ROUTE0  = 198;
    private static final int    TRAIN4_ROUTE1  = 2468;
    private static final int    TRAIN5_ROUTE0  = 3096;
    private static final int    TRAIN5_ROUTE1  = 4798;
    private static final int    TRAIN6_ROUTE0  = 72;
    private static final int    TRAIN6_ROUTE1  = 2298;
    private static final int    TRAIN7_ROUTE0  = 220;
    private static final int    TRAIN7_ROUTE1  = 2320;
    private static final int    TRAIN8_ROUTE0  = 3189;
    private static final int    TRAIN8_ROUTE1  = 4221;
    private static final int    TRAIN9_ROUTE0  = 662;
    private static final int    TRAIN9_ROUTE1  = 1834;
    private static final int    TRAIN10_ROUTE0 = 2535;
    private static final int    TRAIN10_ROUTE1 = 4689;
    private static final int    TRAIN11_ROUTE0 = 810;
    private static final int    TRAIN11_ROUTE1 = 1856;

    @Override
    protected RAS2012Solution fetchSolution() {
        return new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RDS3.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(Itinerary3Test.TRAIN1, new int[] { Itinerary3Test.TRAIN1_ROUTE0,
                Itinerary3Test.TRAIN1_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN2, new int[] { Itinerary3Test.TRAIN2_ROUTE0,
                Itinerary3Test.TRAIN2_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN3, new int[] { Itinerary3Test.TRAIN3_ROUTE0,
                Itinerary3Test.TRAIN3_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN4, new int[] { Itinerary3Test.TRAIN4_ROUTE0,
                Itinerary3Test.TRAIN4_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN5, new int[] { Itinerary3Test.TRAIN5_ROUTE0,
                Itinerary3Test.TRAIN5_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN6, new int[] { Itinerary3Test.TRAIN6_ROUTE0,
                Itinerary3Test.TRAIN6_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN7, new int[] { Itinerary3Test.TRAIN7_ROUTE0,
                Itinerary3Test.TRAIN7_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN8, new int[] { Itinerary3Test.TRAIN8_ROUTE0,
                Itinerary3Test.TRAIN8_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN9, new int[] { Itinerary3Test.TRAIN9_ROUTE0,
                Itinerary3Test.TRAIN9_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN10, new int[] { Itinerary3Test.TRAIN10_ROUTE0,
                Itinerary3Test.TRAIN10_ROUTE1 });
        expected.put(Itinerary3Test.TRAIN11, new int[] { Itinerary3Test.TRAIN11_ROUTE0,
                Itinerary3Test.TRAIN11_ROUTE1 });
        return expected;
    }

    @Override
    protected List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<Itinerary>();
        results.add(this.getItinerary(Itinerary3Test.TRAIN1, Itinerary3Test.TRAIN1_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN1, Itinerary3Test.TRAIN1_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN2, Itinerary3Test.TRAIN2_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN2, Itinerary3Test.TRAIN2_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN3, Itinerary3Test.TRAIN3_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN3, Itinerary3Test.TRAIN3_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN4, Itinerary3Test.TRAIN4_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN4, Itinerary3Test.TRAIN4_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN5, Itinerary3Test.TRAIN5_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN5, Itinerary3Test.TRAIN5_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN6, Itinerary3Test.TRAIN6_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN6, Itinerary3Test.TRAIN6_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN7, Itinerary3Test.TRAIN7_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN7, Itinerary3Test.TRAIN7_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN8, Itinerary3Test.TRAIN8_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN8, Itinerary3Test.TRAIN8_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN9, Itinerary3Test.TRAIN9_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN9, Itinerary3Test.TRAIN9_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN10, Itinerary3Test.TRAIN10_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN10, Itinerary3Test.TRAIN10_ROUTE1));
        results.add(this.getItinerary(Itinerary3Test.TRAIN11, Itinerary3Test.TRAIN11_ROUTE0));
        results.add(this.getItinerary(Itinerary3Test.TRAIN11, Itinerary3Test.TRAIN11_ROUTE1));
        return results;
    }

}

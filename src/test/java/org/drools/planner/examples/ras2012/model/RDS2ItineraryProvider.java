package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.RAS2012ProblemIO;
import org.drools.planner.examples.ras2012.RAS2012Solution;

public class RDS2ItineraryProvider extends ItineraryProvider {

    private static final String TRAIN1         = "A1";
    private static final String TRAIN2         = "A4";
    private static final String TRAIN3         = "B1";
    private static final String TRAIN4         = "B3";
    private static final String TRAIN5         = "C1";
    private static final String TRAIN6         = "C3";
    private static final String TRAIN7         = "D1";
    private static final String TRAIN8         = "E1";
    private static final String TRAIN9         = "E2";
    private static final String TRAIN10        = "F1";
    private static final String TRAIN11        = "F2";

    private static final int    TRAIN1_ROUTE0  = 50;
    private static final int    TRAIN1_ROUTE1  = 2446;
    private static final int    TRAIN2_ROUTE0  = 2519;
    private static final int    TRAIN2_ROUTE1  = 4705;
    private static final int    TRAIN3_ROUTE0  = 198;
    private static final int    TRAIN3_ROUTE1  = 2468;
    private static final int    TRAIN4_ROUTE0  = 2612;
    private static final int    TRAIN4_ROUTE1  = 3288;
    private static final int    TRAIN5_ROUTE0  = 72;
    private static final int    TRAIN5_ROUTE1  = 2298;
    private static final int    TRAIN6_ROUTE0  = 3096;
    private static final int    TRAIN6_ROUTE1  = 4128;
    private static final int    TRAIN7_ROUTE0  = 220;
    private static final int    TRAIN7_ROUTE1  = 2320;
    private static final int    TRAIN8_ROUTE0  = 662;
    private static final int    TRAIN8_ROUTE1  = 1834;
    private static final int    TRAIN9_ROUTE0  = 3189;
    private static final int    TRAIN9_ROUTE1  = 4416;
    private static final int    TRAIN10_ROUTE0 = 122;
    private static final int    TRAIN10_ROUTE1 = 248;
    private static final int    TRAIN11_ROUTE0 = 2535;
    private static final int    TRAIN11_ROUTE1 = 4798;

    @Override
    protected RAS2012Solution fetchSolution() {
        return new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RDS2.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(RDS2ItineraryProvider.TRAIN1, new int[] { RDS2ItineraryProvider.TRAIN1_ROUTE0,
                RDS2ItineraryProvider.TRAIN1_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN2, new int[] { RDS2ItineraryProvider.TRAIN2_ROUTE0,
                RDS2ItineraryProvider.TRAIN2_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN3, new int[] { RDS2ItineraryProvider.TRAIN3_ROUTE0,
                RDS2ItineraryProvider.TRAIN3_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN4, new int[] { RDS2ItineraryProvider.TRAIN4_ROUTE0,
                RDS2ItineraryProvider.TRAIN4_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN5, new int[] { RDS2ItineraryProvider.TRAIN5_ROUTE0,
                RDS2ItineraryProvider.TRAIN5_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN6, new int[] { RDS2ItineraryProvider.TRAIN6_ROUTE0,
                RDS2ItineraryProvider.TRAIN6_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN7, new int[] { RDS2ItineraryProvider.TRAIN7_ROUTE0,
                RDS2ItineraryProvider.TRAIN7_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN8, new int[] { RDS2ItineraryProvider.TRAIN8_ROUTE0,
                RDS2ItineraryProvider.TRAIN8_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN9, new int[] { RDS2ItineraryProvider.TRAIN9_ROUTE0,
                RDS2ItineraryProvider.TRAIN9_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN10, new int[] {
                RDS2ItineraryProvider.TRAIN10_ROUTE0, RDS2ItineraryProvider.TRAIN10_ROUTE1 });
        expected.put(RDS2ItineraryProvider.TRAIN11, new int[] {
                RDS2ItineraryProvider.TRAIN11_ROUTE0, RDS2ItineraryProvider.TRAIN11_ROUTE1 });
        return expected;
    }

    @Override
    public List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<Itinerary>();
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN1,
                RDS2ItineraryProvider.TRAIN1_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN1,
                RDS2ItineraryProvider.TRAIN1_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN2,
                RDS2ItineraryProvider.TRAIN2_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN2,
                RDS2ItineraryProvider.TRAIN2_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN3,
                RDS2ItineraryProvider.TRAIN3_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN3,
                RDS2ItineraryProvider.TRAIN3_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN4,
                RDS2ItineraryProvider.TRAIN4_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN4,
                RDS2ItineraryProvider.TRAIN4_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN5,
                RDS2ItineraryProvider.TRAIN5_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN5,
                RDS2ItineraryProvider.TRAIN5_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN6,
                RDS2ItineraryProvider.TRAIN6_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN6,
                RDS2ItineraryProvider.TRAIN6_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN7,
                RDS2ItineraryProvider.TRAIN7_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN7,
                RDS2ItineraryProvider.TRAIN7_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN8,
                RDS2ItineraryProvider.TRAIN8_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN8,
                RDS2ItineraryProvider.TRAIN8_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN9,
                RDS2ItineraryProvider.TRAIN9_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN9,
                RDS2ItineraryProvider.TRAIN9_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN10,
                RDS2ItineraryProvider.TRAIN10_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN10,
                RDS2ItineraryProvider.TRAIN10_ROUTE1));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN11,
                RDS2ItineraryProvider.TRAIN11_ROUTE0));
        results.add(this.getItinerary(RDS2ItineraryProvider.TRAIN11,
                RDS2ItineraryProvider.TRAIN11_ROUTE1));
        return results;
    }

}

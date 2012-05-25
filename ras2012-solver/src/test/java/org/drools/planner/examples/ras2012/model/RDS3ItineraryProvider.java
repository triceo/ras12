package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.util.SolutionIO;

public class RDS3ItineraryProvider extends ItineraryProvider {

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

    private static final int    TRAIN1_ROUTE0  = 0;
    private static final int    TRAIN1_ROUTE1  = 378;
    private static final int    TRAIN2_ROUTE0  = 1;
    private static final int    TRAIN2_ROUTE1  = 271;
    private static final int    TRAIN3_ROUTE0  = 5;
    private static final int    TRAIN3_ROUTE1  = 367;
    private static final int    TRAIN4_ROUTE0  = 4;
    private static final int    TRAIN4_ROUTE1  = 354;
    private static final int    TRAIN5_ROUTE0  = 9;
    private static final int    TRAIN5_ROUTE1  = 383;
    private static final int    TRAIN6_ROUTE0  = 24;
    private static final int    TRAIN6_ROUTE1  = 382;
    private static final int    TRAIN7_ROUTE0  = 28;
    private static final int    TRAIN7_ROUTE1  = 282;
    private static final int    TRAIN8_ROUTE0  = 17;
    private static final int    TRAIN8_ROUTE1  = 287;
    private static final int    TRAIN9_ROUTE0  = 96;
    private static final int    TRAIN9_ROUTE1  = 358;
    private static final int    TRAIN10_ROUTE0 = 97;
    private static final int    TRAIN10_ROUTE1 = 363;
    private static final int    TRAIN11_ROUTE0 = 120;
    private static final int    TRAIN11_ROUTE1 = 286;

    @Override
    protected RAS2012Solution fetchSolution() {
        return new SolutionIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RDS3.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(RDS3ItineraryProvider.TRAIN1, new int[] { RDS3ItineraryProvider.TRAIN1_ROUTE0,
                RDS3ItineraryProvider.TRAIN1_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN2, new int[] { RDS3ItineraryProvider.TRAIN2_ROUTE0,
                RDS3ItineraryProvider.TRAIN2_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN3, new int[] { RDS3ItineraryProvider.TRAIN3_ROUTE0,
                RDS3ItineraryProvider.TRAIN3_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN4, new int[] { RDS3ItineraryProvider.TRAIN4_ROUTE0,
                RDS3ItineraryProvider.TRAIN4_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN5, new int[] { RDS3ItineraryProvider.TRAIN5_ROUTE0,
                RDS3ItineraryProvider.TRAIN5_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN6, new int[] { RDS3ItineraryProvider.TRAIN6_ROUTE0,
                RDS3ItineraryProvider.TRAIN6_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN7, new int[] { RDS3ItineraryProvider.TRAIN7_ROUTE0,
                RDS3ItineraryProvider.TRAIN7_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN8, new int[] { RDS3ItineraryProvider.TRAIN8_ROUTE0,
                RDS3ItineraryProvider.TRAIN8_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN9, new int[] { RDS3ItineraryProvider.TRAIN9_ROUTE0,
                RDS3ItineraryProvider.TRAIN9_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN10, new int[] {
                RDS3ItineraryProvider.TRAIN10_ROUTE0, RDS3ItineraryProvider.TRAIN10_ROUTE1 });
        expected.put(RDS3ItineraryProvider.TRAIN11, new int[] {
                RDS3ItineraryProvider.TRAIN11_ROUTE0, RDS3ItineraryProvider.TRAIN11_ROUTE1 });
        return expected;
    }

    @Override
    public List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<Itinerary>();
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN1,
                RDS3ItineraryProvider.TRAIN1_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN1,
                RDS3ItineraryProvider.TRAIN1_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN2,
                RDS3ItineraryProvider.TRAIN2_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN2,
                RDS3ItineraryProvider.TRAIN2_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN3,
                RDS3ItineraryProvider.TRAIN3_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN3,
                RDS3ItineraryProvider.TRAIN3_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN4,
                RDS3ItineraryProvider.TRAIN4_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN4,
                RDS3ItineraryProvider.TRAIN4_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN5,
                RDS3ItineraryProvider.TRAIN5_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN5,
                RDS3ItineraryProvider.TRAIN5_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN6,
                RDS3ItineraryProvider.TRAIN6_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN6,
                RDS3ItineraryProvider.TRAIN6_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN7,
                RDS3ItineraryProvider.TRAIN7_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN7,
                RDS3ItineraryProvider.TRAIN7_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN8,
                RDS3ItineraryProvider.TRAIN8_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN8,
                RDS3ItineraryProvider.TRAIN8_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN9,
                RDS3ItineraryProvider.TRAIN9_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN9,
                RDS3ItineraryProvider.TRAIN9_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN10,
                RDS3ItineraryProvider.TRAIN10_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN10,
                RDS3ItineraryProvider.TRAIN10_ROUTE1));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN11,
                RDS3ItineraryProvider.TRAIN11_ROUTE0));
        results.add(this.getItinerary(RDS3ItineraryProvider.TRAIN11,
                RDS3ItineraryProvider.TRAIN11_ROUTE1));
        return results;
    }

}

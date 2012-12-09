package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.util.SolutionIO;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RDS1ItineraryProvider extends ItineraryProvider {

    private static final String TRAIN1        = "F1";
    private static final String TRAIN2        = "E1";
    private static final String TRAIN3        = "D1";
    private static final String TRAIN4        = "C1";
    private static final String TRAIN5        = "B2";
    private static final String TRAIN6        = "B1";
    private static final String TRAIN7        = "A1";

    private static final int    TRAIN7_ROUTE0 = 12;
    private static final int    TRAIN7_ROUTE1 = 362;
    private static final int    TRAIN6_ROUTE0 = 1;
    private static final int    TRAIN6_ROUTE1 = 383;
    private static final int    TRAIN5_ROUTE0 = 108;
    private static final int    TRAIN5_ROUTE1 = 266;
    private static final int    TRAIN4_ROUTE0 = 14;
    private static final int    TRAIN4_ROUTE1 = 282;
    private static final int    TRAIN3_ROUTE0 = 17;
    private static final int    TRAIN3_ROUTE1 = 145;
    private static final int    TRAIN2_ROUTE0 = 20;
    private static final int    TRAIN2_ROUTE1 = 170;
    private static final int    TRAIN1_ROUTE0 = 5;
    private static final int    TRAIN1_ROUTE1 = 379;

    @Override
    protected ProblemSolution fetchSolution() {
        return new SolutionIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RDS1.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<>();
        expected.put(RDS1ItineraryProvider.TRAIN1, new int[] { RDS1ItineraryProvider.TRAIN1_ROUTE0,
                RDS1ItineraryProvider.TRAIN1_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN2, new int[] { RDS1ItineraryProvider.TRAIN2_ROUTE0,
                RDS1ItineraryProvider.TRAIN2_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN3, new int[] { RDS1ItineraryProvider.TRAIN3_ROUTE0,
                RDS1ItineraryProvider.TRAIN3_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN4, new int[] { RDS1ItineraryProvider.TRAIN4_ROUTE0,
                RDS1ItineraryProvider.TRAIN4_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN5, new int[] { RDS1ItineraryProvider.TRAIN5_ROUTE0,
                RDS1ItineraryProvider.TRAIN5_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN6, new int[] { RDS1ItineraryProvider.TRAIN6_ROUTE0,
                RDS1ItineraryProvider.TRAIN6_ROUTE1 });
        expected.put(RDS1ItineraryProvider.TRAIN7, new int[] { RDS1ItineraryProvider.TRAIN7_ROUTE0,
                RDS1ItineraryProvider.TRAIN7_ROUTE1 });
        return expected;
    }

    @Override
    public List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<>();
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN1,
                RDS1ItineraryProvider.TRAIN1_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN1,
                RDS1ItineraryProvider.TRAIN1_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN2,
                RDS1ItineraryProvider.TRAIN2_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN2,
                RDS1ItineraryProvider.TRAIN2_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN3,
                RDS1ItineraryProvider.TRAIN3_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN3,
                RDS1ItineraryProvider.TRAIN3_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN4,
                RDS1ItineraryProvider.TRAIN4_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN4,
                RDS1ItineraryProvider.TRAIN4_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN5,
                RDS1ItineraryProvider.TRAIN5_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN5,
                RDS1ItineraryProvider.TRAIN5_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN6,
                RDS1ItineraryProvider.TRAIN6_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN6,
                RDS1ItineraryProvider.TRAIN6_ROUTE1));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN7,
                RDS1ItineraryProvider.TRAIN7_ROUTE0));
        results.add(this.getItinerary(RDS1ItineraryProvider.TRAIN7,
                RDS1ItineraryProvider.TRAIN7_ROUTE1));
        return results;
    }

}

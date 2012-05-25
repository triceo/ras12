package org.drools.planner.examples.ras2012.model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.util.SolutionIO;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ToyItineraryProvider extends ItineraryProvider {

    private static final String TRAIN1        = "C1";
    private static final String TRAIN2        = "B1";
    private static final String TRAIN3        = "A1";

    private static final int    TRAIN1_ROUTE0 = 2;
    private static final int    TRAIN1_ROUTE1 = 0;
    private static final int    TRAIN2_ROUTE0 = 7;
    private static final int    TRAIN2_ROUTE1 = 1;
    private static final int    TRAIN3_ROUTE0 = 4;

    @Override
    protected RAS2012Solution fetchSolution() {
        return new SolutionIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/TOY.txt"));
    }

    @Override
    protected Map<String, int[]> getExpectedValues() {
        final Map<String, int[]> expected = new HashMap<String, int[]>();
        expected.put(ToyItineraryProvider.TRAIN1, new int[] { ToyItineraryProvider.TRAIN1_ROUTE0,
                ToyItineraryProvider.TRAIN1_ROUTE1 });
        expected.put(ToyItineraryProvider.TRAIN2, new int[] { ToyItineraryProvider.TRAIN2_ROUTE0,
                ToyItineraryProvider.TRAIN2_ROUTE1 });
        expected.put(ToyItineraryProvider.TRAIN3, new int[] { ToyItineraryProvider.TRAIN3_ROUTE0 });
        return expected;
    }

    @Override
    public List<Itinerary> getItineraries() {
        final List<Itinerary> results = new LinkedList<Itinerary>();
        results.add(this.getItinerary(ToyItineraryProvider.TRAIN1,
                ToyItineraryProvider.TRAIN1_ROUTE0));
        results.add(this.getItinerary(ToyItineraryProvider.TRAIN1,
                ToyItineraryProvider.TRAIN1_ROUTE1));
        results.add(this.getItinerary(ToyItineraryProvider.TRAIN2,
                ToyItineraryProvider.TRAIN2_ROUTE0));
        results.add(this.getItinerary(ToyItineraryProvider.TRAIN2,
                ToyItineraryProvider.TRAIN2_ROUTE1));
        results.add(this.getItinerary(ToyItineraryProvider.TRAIN3,
                ToyItineraryProvider.TRAIN3_ROUTE0));
        return results;
    }

}

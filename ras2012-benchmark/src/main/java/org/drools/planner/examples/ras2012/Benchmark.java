package org.drools.planner.examples.ras2012;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.drools.planner.benchmark.api.PlannerBenchmark;
import org.drools.planner.benchmark.config.XmlPlannerBenchmarkFactory;
import org.drools.planner.examples.ras2012.model.ItineraryAssignment;

public class Benchmark {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getTemplateData() {
        int[] hardSA = new int[] { 0, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        int[] softSA = new int[] { 0, 5, 10, 50, 100, 500, 1000, 5000, 10000 };
        Collection<int[]> SAs = new LinkedList<int[]>();
        for (int hard : hardSA) {
            for (int soft : softSA) {
                SAs.add(new int[] { hard, soft });
            }
        }
        Map result = new HashMap();
        result.put("sa", SAs);
        return result;
    }

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        final XmlPlannerBenchmarkFactory fact = new XmlPlannerBenchmarkFactory();
        fact.addXstreamAnnotations(ProblemSolution.class);
        fact.addXstreamAnnotations(ItineraryAssignment.class);
        fact.configureFromTemplate(Benchmark.class.getResourceAsStream("/benchmark-config.ftl"),
                getTemplateData());
        final PlannerBenchmark inst = fact.buildPlannerBenchmark();
        inst.benchmark();
    }
}

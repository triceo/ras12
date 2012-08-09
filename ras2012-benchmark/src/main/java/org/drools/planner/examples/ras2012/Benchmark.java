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
        int[] hardSA = new int[] { 10, 20, 30, 80, 90, 100 };
        int[] softSA = new int[] { 10, 100, 1000 };
        int[] selections = new int[] { 8 };
        int[] moveTabus = new int[] { 1 };
        int[] solutionTabus = new int[] { 1000 };
        int[] probabilities = new int[] { 2, 3, 4, 8, 9, 10 };
        Collection<int[]> SAs = new LinkedList<int[]>();
        for (int hard : hardSA) {
            for (int soft : softSA) {
                for (int selection : selections) {
                    for (int moveTabu : moveTabus) {
                        for (int solutionTabu : solutionTabus) {
                            for (int prob : probabilities) {
                                SAs.add(new int[] { hard, soft, selection, moveTabu, solutionTabu,
                                        prob });
                            }
                        }
                    }
                }
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

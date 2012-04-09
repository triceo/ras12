package org.drools.planner.examples.ras2012;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.drools.planner.benchmark.api.PlannerBenchmark;
import org.drools.planner.benchmark.config.XmlPlannerBenchmarkFactory;
import org.drools.planner.examples.ras2012.model.planner.ItineraryAssignment;

/**
 * Hello world!
 * 
 */
public class Benchmark {

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        XmlPlannerBenchmarkFactory fact = new XmlPlannerBenchmarkFactory();
        fact.addXstreamAnnotations(RAS2012Solution.class);
        fact.addXstreamAnnotations(ItineraryAssignment.class);
        fact.configure("/benchmarkConfig.xml");
        PlannerBenchmark inst = fact.buildPlannerBenchmark();
        inst.benchmark();
    }
}

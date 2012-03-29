package org.drools.planner.examples.ras2012;

import java.io.File;

/**
 * Hello world!
 * 
 */
public class App {
    public static void main(final String[] args) {
        final RAS2012Solution sol = new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RAS DATA SET 1.txt"));
        System.out.println(sol);
    }
}

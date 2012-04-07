package org.drools.planner.examples.ras2012;

import java.io.File;

import org.drools.planner.config.XmlSolverFactory;
import org.drools.planner.core.Solver;

/**
 * Hello world!
 * 
 */
public class App {
    public static void main(final String[] args) {
        RAS2012Solution sol = new RAS2012ProblemIO().read(new File(
                "src/main/resources/org/drools/planner/examples/ras2012/RAS DATA SET 1.txt"));
        final XmlSolverFactory configurer = new XmlSolverFactory();
        configurer.configure(App.class.getResourceAsStream("/solverConfig.xml"));
        Solver solver = configurer.buildSolver();
        solver.setPlanningProblem(sol);
        solver.solve();
        System.out.println(solver.getBestSolution());
    }
}

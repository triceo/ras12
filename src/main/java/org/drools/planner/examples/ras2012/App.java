package org.drools.planner.examples.ras2012;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.drools.planner.config.XmlSolverFactory;
import org.drools.planner.core.Solver;

/**
 * Hello world!
 * 
 */
public class App {

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        // read solution
        final File f = new File("src/main/resources/org/drools/planner/examples/ras2012/RDS3.txt");
        final RAS2012Solution sol = new RAS2012ProblemIO().read(f);
        // and now start solving
        final XmlSolverFactory configurer = new XmlSolverFactory();
        configurer.configure(App.class.getResourceAsStream("/solverConfig.xml"));
        final Solver solver = configurer.buildSolver();
        solver.setPlanningProblem(sol);
        solver.solve();
        // output the solution
        final File targetFolder = new File("data/solutions");
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        RAS2012Solution solution = (RAS2012Solution) solver.getBestSolution();
        new RAS2012ProblemIO().write(solution, new File(targetFolder, f.getName()));
        solution.visualize(new File(targetFolder, f.getName() + ".png"));
    }
}

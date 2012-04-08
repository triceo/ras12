package org.drools.planner.examples.ras2012;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.drools.planner.config.XmlSolverFactory;
import org.drools.planner.core.Solver;
import org.drools.planner.examples.ras2012.model.Network;
import org.drools.planner.examples.ras2012.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 * 
 */
public class App {

    private static class Visualizer implements Callable<Boolean> {

        private static final Logger logger = LoggerFactory.getLogger(Visualizer.class);

        private final Network       network;
        private final File          file;

        public Visualizer(final File dataset, final Network n) {
            this.network = n;
            this.file = dataset;
        }

        @Override
        public Boolean call() {
            Visualizer.logger.info("Started visualization work.");
            try {
                // visualize the network and all the routes
                final File parentFolder = new File("data", this.file.getName());
                if (!parentFolder.exists()) {
                    parentFolder.mkdirs();
                }
                Visualizer.logger.debug("Started visualizing the network.");
                FileOutputStream fos = new FileOutputStream(new File(parentFolder, "network.png"));
                this.network.visualize(fos);
                fos.close();
                final Collection<Route> routes = new LinkedList<Route>();
                routes.addAll(this.network.getAllEastboundRoutes());
                routes.addAll(this.network.getAllWestboundRoutes());
                for (final Route r : routes) {
                    Visualizer.logger.debug("Started visualizing route " + r.getId());
                    fos = new FileOutputStream(new File(parentFolder, r.getId() + ".png"));
                    r.visualize(fos);
                    fos.close();
                }
                Visualizer.logger.info("Finished visualization work.");
                return true;
            } catch (final IOException ex) {
                Visualizer.logger.warn("Visualizing failed.", ex);
                return false;
            }
        }

    }

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        // read solution
        final File f = new File("src/main/resources/org/drools/planner/examples/ras2012/RDS1.txt");
        final RAS2012Solution sol = new RAS2012ProblemIO().read(f);
        final Future<Boolean> visualizationSuccess = Executors.newCachedThreadPool().submit(
                new Visualizer(f, sol.getNetwork()));
        // and now start solving
        final XmlSolverFactory configurer = new XmlSolverFactory();
        configurer.configure(App.class.getResourceAsStream("/solverConfig.xml"));
        final Solver solver = configurer.buildSolver();
        solver.setPlanningProblem(sol);
        solver.solve();
        System.out.println(solver.getBestSolution());
        if (!visualizationSuccess.isDone()) {
            App.logger.info("Waiting for visualizations to finish.");
        }

    }
}

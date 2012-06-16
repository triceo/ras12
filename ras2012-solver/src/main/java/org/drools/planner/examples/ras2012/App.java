package org.drools.planner.examples.ras2012;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.drools.planner.config.XmlSolverFactory;
import org.drools.planner.core.Solver;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.examples.ras2012.CLI.ApplicationMode;
import org.drools.planner.examples.ras2012.util.SolutionIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 * 
 */
public class App {

    private static class SolverRunner implements Runnable {

        private final InputStream dataset;
        private final String      name;

        public SolverRunner(final InputStream dataset, final String name) {
            this.dataset = dataset;
            this.name = name;
        }

        @Override
        public void run() {
            App.logger.info(this.name + " solver starting...");
            final SolutionIO io = new SolutionIO();
            ProblemSolution sol;
            try {
                sol = io.read(this.dataset);
            } catch (final Exception e) {
                App.logger.error("Solver " + this.name + " died. Cause: ", e);
                return;
            }
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
            sol = (ProblemSolution) solver.getBestSolution();
            final HardAndSoftScore score = sol.getScore();
            if (score.getHardScore() >= 0) { // don't write score that isn't feasible
                io.writeXML(sol, new File(targetFolder, this.name + score.getSoftScore() + ".xml"));
                io.writeTex(sol, new File(targetFolder, this.name + score.getSoftScore() + ".tex"));
                sol.visualize(new File(targetFolder, this.name + score.getSoftScore() + ".png"));
                App.logger.warn("Solver finished. Score: " + score);
            } else {
                App.logger.warn("Not writing results because solution wasn't feasible: " + score);
            }
        }

    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1,
                                                          Runtime.getRuntime()
                                                                  .availableProcessors() - 1));
    private static final Logger          logger   = LoggerFactory.getLogger(App.class);

    public static void main(final String[] args) {
        final CLI commandLine = CLI.getInstance();
        final ApplicationMode result = commandLine.process(args);
        switch (result) {
            case RESOLVER:
                App.runSolverMode(commandLine.getDatasetLocation());
                break;
            case LOOKUP:
                App.runLookupMode();
                break;
            case EVALUATION:
                App.runEvaluationMode(commandLine.getDatasetLocation(),
                        commandLine.getSolutionLocation());
                break;
            case HELP:
            default:
                commandLine.printHelp();
        }
        if (result == ApplicationMode.ERROR) {
            System.exit(1);
        }
    }

    private static void runEvaluationMode(final String datasetLocation,
            final String solutionLocation) {
        // validate data
        final File dataset = new File(datasetLocation);
        if (!dataset.exists() || !dataset.canRead()) {
            throw new IllegalArgumentException("Cannot read data set: " + dataset);
        }
        final File solution = new File(datasetLocation);
        if (!solution.exists() || !solution.canRead()) {
            throw new IllegalArgumentException("Cannot read data set: " + solution);
        }
        // load solution
        final SolutionIO io = new SolutionIO();
        final ProblemSolution result = io.read(dataset, solution);
        App.logger.info("Solution " + result.getName() + " has a score of "
                + ScoreCalculator.oneTimeCalculation(result) + ".");
    }

    private static void runLookupMode() {
        final Set<String> streams = new HashSet<String>();
        streams.add("RDS1");
        streams.add("RDS2");
        streams.add("RDS3");
        streams.add("TOY");
        for (final String entry : streams) {
            App.logger.info("Starting lookup for the best solutions on " + entry + "...");
            for (int i = 0; i < 20; i++) {
                App.logger.info("Scheduled attempt #" + i + ".");
                App.executor.execute(new SolverRunner(
                        App.class.getResourceAsStream(entry + ".txt"), entry));
            }
        }
        App.shutdownExecutor();
    }

    private static void runSolverMode(final String datasetLocation) {
        final File f = new File(datasetLocation);
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Cannot read data set: " + f);
        }
        try {
            App.executor.submit(new SolverRunner(new FileInputStream(f), f.getName()));
            App.shutdownExecutor();
        } catch (final FileNotFoundException e) {
            App.logger.warn(f.getName() + " solver not started. Cause: ", e);
        }
    }

    private static void shutdownExecutor() {
        App.executor.shutdown();
        while (!App.executor.isTerminated()) {
            try {
                App.executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                // nothing we could do here
            }
        }
    }
}

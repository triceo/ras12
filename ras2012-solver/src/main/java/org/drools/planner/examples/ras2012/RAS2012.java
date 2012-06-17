package org.drools.planner.examples.ras2012;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.drools.planner.config.XmlSolverFactory;
import org.drools.planner.core.Solver;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.examples.ras2012.CLI.ApplicationMode;
import org.drools.planner.examples.ras2012.util.Chart;
import org.drools.planner.examples.ras2012.util.SolutionIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the application. Supports multiple modes of execution:
 * 
 * <ul>
 * <li>The solver mode takes a user-provided data set, runs Drools Planner and outputs the result.</li>
 * <li>The lookup mode tries to find best solutions for every known data set.</li>
 * <li>The evaluation mode takes a user-provided data set and a solution from that data set and calculates its score.</li>
 * </ul>
 * 
 * Modes of execution are chosen by providing an argument on the command line. List of command line arguments will be shown when
 * the application is run without any arguments.
 */
public class RAS2012 {

    /**
     * Executes Drools planner on a particular data set.
     */
    private static class SolverRunner implements Callable<HardAndSoftScore> {

        private final InputStream dataset;
        private final String      name;

        /**
         * 
         * @param dataset The data set to execute Drools Planner on.
         * @param name Name of the data set.
         */
        public SolverRunner(final InputStream dataset, final String name) {
            this.dataset = dataset;
            this.name = name;
        }

        @Override
        public HardAndSoftScore call() {
            RAS2012.logger.info(this.name + " solver starting...");
            final SolutionIO io = new SolutionIO();
            ProblemSolution sol;
            try {
                sol = io.read(this.dataset);
            } catch (final Exception e) {
                RAS2012.logger.error("Solver " + this.name + " finished unexpectedly. Cause: ", e);
                return null;
            }
            // and now start solving
            final XmlSolverFactory configurer = new XmlSolverFactory();
            configurer.configure(RAS2012.class.getResourceAsStream("/solverConfig.xml"));
            final Solver solver = configurer.buildSolver();
            solver.setPlanningProblem(sol);
            solver.solve();
            // output the solution
            if (!RAS2012.resultDir.exists()) {
                RAS2012.resultDir.mkdirs();
            }
            sol = (ProblemSolution) solver.getBestSolution();
            final HardAndSoftScore score = sol.getScore();
            if (score.getHardScore() >= 0) { // don't write score that isn't feasible
                io.writeXML(sol, new File(RAS2012.resultDir, this.name + score.getSoftScore()
                        + ".xml"));
                io.writeTex(sol, new File(RAS2012.resultDir, this.name + score.getSoftScore()
                        + ".tex"));
                sol.visualize(new File(RAS2012.resultDir, this.name + score.getSoftScore() + ".png"));
                RAS2012.logger.info("Solver finished. Score: " + score);
            } else {
                RAS2012.logger.warn("Not writing results because solution wasn't feasible: "
                        + score);
            }
            return score;
        }

    }

    private static final File            resultDir = new File("data/solutions");

    /**
     * Only use up to 2 threads, otherwise the increased GC would negatively impact performance.
     */
    private static final ExecutorService executor  = Executors.newFixedThreadPool(Math.min(2,
                                                           Runtime.getRuntime()
                                                                   .availableProcessors()));
    private static final Logger          logger    = LoggerFactory.getLogger(RAS2012.class);

    /**
     * Main method of the whole app. Use for launching the app.
     * 
     * @param args Command-line arguments.
     */
    public static void main(final String[] args) {
        final CLI commandLine = CLI.getInstance();
        final ApplicationMode result = commandLine.process(args);
        switch (result) {
            case RESOLVER:
                RAS2012.runSolverMode(commandLine.getDatasetLocation());
                break;
            case LOOKUP:
                RAS2012.runLookupMode();
                break;
            case EVALUATION:
                RAS2012.runEvaluationMode(commandLine.getDatasetLocation(),
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
        RAS2012.logger.info("Solution " + result.getName() + " has a score of "
                + ScoreCalculator.oneTimeCalculation(result) + ".");
    }

    private static void runLookupMode() {
        final Set<String> streams = new HashSet<String>();
        streams.add("RDS1");
        streams.add("RDS2");
        streams.add("RDS3");
        streams.add("TOY");
        // prepare futures
        final Map<String, List<Future<HardAndSoftScore>>> scores = new HashMap<String, List<Future<HardAndSoftScore>>>();
        for (final String entry : streams) {
            RAS2012.logger.info("Starting lookup for the best solutions on " + entry + "...");
            scores.put(entry, new ArrayList<Future<HardAndSoftScore>>());
            for (int i = 0; i < 1; i++) {
                RAS2012.logger.info("Scheduled attempt #" + i + ".");
                scores.get(entry).add(
                        RAS2012.executor.submit(new SolverRunner(RAS2012.class
                                .getResourceAsStream(entry + ".txt"), entry)));
            }
        }
        // prepare chart
        final Chart c = new Chart();
        // process futures
        for (final Map.Entry<String, List<Future<HardAndSoftScore>>> entry : scores.entrySet()) {
            final String datasetName = entry.getKey();
            final List<Integer> values = new ArrayList<Integer>();
            for (final Future<HardAndSoftScore> future : entry.getValue()) {
                try {
                    final HardAndSoftScore result = future.get();
                    if (result != null && result.getHardScore() >= 0) {
                        values.add(Math.abs(result.getSoftScore()));
                    }
                } catch (final Exception e) {
                    RAS2012.logger.error("One of the solvers failed.");
                }
            }
            c.addData(values, datasetName);
        }
        // plot chart
        c.plot(RAS2012.resultDir, "chart");
        new SolutionIO().writeChart(c.getDataset(), new File(RAS2012.resultDir, "stats.tex"));
        RAS2012.shutdownExecutor();
    }

    private static void runSolverMode(final String datasetLocation) {
        final File f = new File(datasetLocation);
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Cannot read data set: " + f);
        }
        try {
            RAS2012.executor.submit(new SolverRunner(new FileInputStream(f), f.getName()));
            RAS2012.shutdownExecutor();
        } catch (final FileNotFoundException e) {
            RAS2012.logger.warn(f.getName() + " solver not started. Cause: ", e);
        }
    }

    private static void shutdownExecutor() {
        RAS2012.executor.shutdown();
        while (!RAS2012.executor.isTerminated()) {
            try {
                RAS2012.executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                // nothing we could do here
            }
        }
    }
}

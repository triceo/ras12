package org.drools.planner.examples.ras2012;

import java.io.File;

import org.drools.planner.benchmark.api.ProblemIO;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.examples.ras2012.util.SolutionIO;

public class RAS2012ProblemIO implements ProblemIO {

    private final SolutionIO io = new SolutionIO();

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public ProblemSolution read(final File inputSolutionFile) {
        return this.io.read(inputSolutionFile);
    }

    @Override
    public void write(@SuppressWarnings("rawtypes") final Solution solution,
            final File outputSolutionFile) {
        this.io.writeXML((ProblemSolution) solution, outputSolutionFile);
    }
}

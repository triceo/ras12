package org.drools.planner.examples.ras2012.util;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.drools.planner.core.score.buildin.hardandsoft.DefaultHardAndSoftScore;
import org.drools.planner.core.score.buildin.hardandsoft.HardAndSoftScore;
import org.drools.planner.examples.ras2012.ProblemSolution;
import org.drools.planner.examples.ras2012.ScoreCalculator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SolutionIOTest {

    private static enum SolutionType {

        RDS1("/org/drools/planner/examples/ras2012/parser/validParserInput1.txt"), RDS2(
                "/org/drools/planner/examples/ras2012/parser/validParserInput2.txt"), RDS3(
                "/org/drools/planner/examples/ras2012/parser/validParserInput3.txt");

        private final String definition;

        SolutionType(final String definition) {
            this.definition = definition;
        }

        public InputStream getDefinition() {
            return SolutionIOTest.class.getResourceAsStream(this.definition);
        }
    }

    private static Object[] getResource(final SolutionType solutionType, final int hard,
            final int soft) {
        return new Object[] {
                solutionType.getDefinition(),
                SolutionIOTest.class.getResourceAsStream(solutionType.name() + ".txt" + soft
                        + ".xml"), hard, soft };
    }

    @Parameters
    public static Collection<Object[]> getResources() {
        final List<Object[]> resources = new LinkedList<Object[]>();
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -1280));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -1848));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2107));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2146));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -3030));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -3478));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -3541));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -3694));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -4027));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -4132));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -11115));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -11316));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -12151));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -14615));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -17827));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -7867));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -9228));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -9695));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -9954));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -12873));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -12942));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -13460));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -14030));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -14855));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -15605));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -15848));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -16326));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -16747));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -17000));
        return resources;
    }

    private final InputStream      solution, definition;
    private final HardAndSoftScore score;

    private static final File      parent = new File("data/tests/"
                                                  + SolutionIOTest.class.getCanonicalName());

    public SolutionIOTest(final InputStream solutionDefinition, final InputStream solution,
            final int hardScore, final int softScore) {
        this.definition = solutionDefinition;
        this.solution = solution;
        this.score = DefaultHardAndSoftScore.valueOf(hardScore, softScore);
    }

    @Test
    public void testSolutionValidation() {
        final SolutionIO io = new SolutionIO();
        final ProblemSolution result = io.read(this.definition, this.solution);
        if (!SolutionIOTest.parent.exists()) {
            SolutionIOTest.parent.mkdirs();
        }
        io.writeXML(result,
                new File(SolutionIOTest.parent, result.getName() + this.score.getSoftScore()
                        + ".xml"));
        Assert.assertEquals(this.score, ScoreCalculator.oneTimeCalculation(result));
        // FIXME compare the two files
    }
}

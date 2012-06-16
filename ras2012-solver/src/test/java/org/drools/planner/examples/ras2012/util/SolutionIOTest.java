package org.drools.planner.examples.ras2012.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

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
        return new Object[] { solutionType.getDefinition(),
                solutionType.name() + ".txt" + soft + ".xml", hard, soft };
    }

    @Parameters
    public static Collection<Object[]> getResources() {
        final List<Object[]> resources = new LinkedList<Object[]>();
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -1160));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -1459));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -1570));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2271));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2623));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2673));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -2943));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -5253));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -5502));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS1, 0, -7124));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -8764));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -8822));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -10767));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -11340));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -11483));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -13049));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -15089));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -16135));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -17326));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS2, 0, -18031));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -11669));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -15144));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -15524));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -16093));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -16671));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -17307));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -17367));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -18026));
        resources.add(SolutionIOTest.getResource(SolutionType.RDS3, 0, -19966));
        return resources;
    }

    private final InputStream      definition;
    private final String           solutionResource;
    private final HardAndSoftScore score;

    private static final File      parent = new File("data/tests/"
                                                  + SolutionIOTest.class.getCanonicalName());

    public SolutionIOTest(final InputStream solutionDefinition, final String solution,
            final int hardScore, final int softScore) {
        this.definition = solutionDefinition;
        this.solutionResource = solution;
        this.score = DefaultHardAndSoftScore.valueOf(hardScore, softScore);
    }

    @SuppressWarnings("resource")
    @Test
    public void testSolutionValidation() throws IOException {
        final SolutionIO io = new SolutionIO();
        final ProblemSolution result = io.read(this.definition,
                SolutionIOTest.class.getResourceAsStream(this.solutionResource));
        if (!SolutionIOTest.parent.exists()) {
            SolutionIOTest.parent.mkdirs();
        }
        // compare XML results
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        io.writeXML(result, baos);
        String oldContent = new Scanner(
                SolutionIOTest.class.getResourceAsStream(this.solutionResource))
                .useDelimiter("\\A").next();
        String newContent = baos.toString();
        File f = new File(SolutionIOTest.parent, result.getName() + this.score.getSoftScore()
                + ".xml");
        io.writeXML(result, f);
        Assert.assertTrue("XML files do not match. Check " + f, newContent.equals(oldContent));
        // compare scores
        Assert.assertEquals("Scores don't match, even though XML files do.", this.score,
                ScoreCalculator.oneTimeCalculation(result));
    }
}

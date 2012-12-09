package org.drools.planner.examples.ras2012.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParseValidInputTest {

    @Parameters
    public static Collection<Object[]> parserTestFiles() {
        final List<Object[]> streams = new ArrayList<>();
        int i = 1;
        while (true) {
            final String name = "validParserInput" + i + ".txt";
            final InputStream is = ParseValidInputTest.class.getResourceAsStream(name);
            if (is == null) {
                break;
            }
            streams.add(new Object[] { is, name });
            i++;
        }
        return streams;
    }

    private final InputStream source;

    private final String      name;

    public ParseValidInputTest(final InputStream is, final String name) {
        this.source = is;
        this.name = name;
    }

    @Test
    public void testParse() {
        final DataSetParser p = new DataSetParser(this.source);
        try {
            p.parse();
        } catch (final ParseException ex) {
            Assert.fail("Parsing source file '" + this.name + "' failed: " + ex.getMessage());
        } catch (final TokenMgrError ex) {
            Assert.fail("Analyzing source file '" + this.name + "' failed: " + ex.getMessage());
        } finally {
        }
    }

}

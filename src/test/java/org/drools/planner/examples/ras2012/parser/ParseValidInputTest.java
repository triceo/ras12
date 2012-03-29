package org.drools.planner.examples.ras2012.parser;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized.Parameters;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import org.junit.Test;

@RunWith(Parameterized.class)
public class ParseValidInputTest {
    
    private final InputStream source;
    private final String name;

    @Parameters
    public static Collection<Object[]> parserTestFiles() {
        List<Object[]> streams = new ArrayList<Object[]>();
        int i = 1;
        while (true) {
            String name = "validParserInput" + i + ".txt";
            InputStream is = ParseValidInputTest.class.getResourceAsStream(name);
            if (is == null) break;
            streams.add(new Object[] {is, name});
            i++;
        }
        return streams;
    }
    
    public ParseValidInputTest(InputStream is, String name) {
        this.source = is;
        this.name = name;
    }
    
    @Test
    public void testParse() {
        DataSetParser p = new DataSetParser(this.source);
        try {
          p.parse();
        } catch (ParseException ex) {
            fail("Parsing source file '" + this.name + "' failed: " + ex.getMessage());
        } catch (TokenMgrError ex) {
            fail("Analyzing source file '" + this.name + "' failed: " + ex.getMessage());
        } finally {
        }
    }

}

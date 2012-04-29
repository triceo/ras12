package org.drools.planner.examples.ras2012.model.original;

import java.math.BigDecimal;
import java.util.Collections;

import org.drools.planner.examples.ras2012.model.Route;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractSectionTest {

    @Test
    public abstract void testInitialAndTerminalNodesOnTrain();

    @Test
    public abstract void testInitialAndTerminalNodesOnRoute();

    protected void actuallyTestInitialAndTerminalNodesOnRoute(Section s) {
        // prepare routes, one eastbound and one westbound
        final Route eastbound = new Route(true);
        final Route westbound = new Route(false);
        // and validate their starting and ending nodes
        Assert.assertSame(s.getWestNode(), s.getOrigin(eastbound));
        Assert.assertSame(s.getEastNode(), s.getDestination(eastbound));
        Assert.assertSame(s.getEastNode(), s.getOrigin(westbound));
        Assert.assertSame(s.getWestNode(), s.getDestination(westbound));
    }

    protected void actuallyTestInitialAndTerminalNodesOnTrain(Section s) {
        // prepare routes, one eastbound and one westbound
        final BigDecimal length = new BigDecimal("1.5");
        final Train westbound = new Train("A1", BigDecimal.ONE, length, 90, s.getEastNode(),
                s.getWestNode(), 0, 0, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, true);
        final Train eastbound = new Train("A2", BigDecimal.ONE, length, 90, s.getWestNode(),
                s.getEastNode(), 0, 0, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, false);
        // and validate their starting and ending nodes
        Assert.assertSame(s.getWestNode(), s.getOrigin(eastbound));
        Assert.assertSame(s.getEastNode(), s.getDestination(eastbound));
        Assert.assertSame(s.getEastNode(), s.getOrigin(westbound));
        Assert.assertSame(s.getWestNode(), s.getDestination(westbound));
    }

}

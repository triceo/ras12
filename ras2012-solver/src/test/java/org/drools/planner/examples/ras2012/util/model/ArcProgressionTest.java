package org.drools.planner.examples.ras2012.util.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Route.Builder;
import org.drools.planner.examples.ras2012.model.Track;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ArcProgressionTest {

    private static int factorial(final int i) {
        if (i == 0) {
            return 1;
        } else {
            return i * ArcProgressionTest.factorial(i - 1);
        }
    }

    /**
     * Run the test for both eastbound and westbound directions.
     * 
     * @return
     */
    @Parameters
    public static Collection<Object[]> getDirections() {
        final Collection<Object[]> directions = new ArrayList<>();
        directions.add(new Builder[] { new Builder(false) });
        directions.add(new Builder[] { new Builder(true) });
        return directions;
    }

    private final Route   route;
    private final Builder builder;

    public ArcProgressionTest(final Builder b) {
        this.builder = b;
        this.route = b.build();
    }

    @Test
    public void testConstructor() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc b = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Arc c = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(2), Node.getNode(3));
        final Arc d = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(3), Node.getNode(4));
        final List<Arc> arcs = Arrays.asList(new Arc[] { a, b, c, d });
        final List<Arc> eastboundOrdering = Arrays.asList(new Arc[] { a, b, c, d });
        final List<Arc> westboundOrdering = Arrays.asList(new Arc[] { d, c, b, a });
        for (int i = 0; i < ArcProgressionTest.factorial(arcs.size()); i++) {
            Collections.shuffle(arcs); // randomly re-group arcs to validate proper ordering
            final ArcProgression progression = new ArcProgression(this.route, arcs);
            // validate directions
            Assert.assertEquals("Progression's direction must match source.",
                    this.route.isEastbound(), progression.isEastbound());
            Assert.assertEquals("Progression's direction must match source.",
                    this.route.isWestbound(), progression.isWestbound());
            // validate ordering of arcs
            Assert.assertEquals(this.route.isEastbound() ? eastboundOrdering : westboundOrdering,
                    progression.getArcs());
            // validate nodes
            final List<Node> nodes = Arrays.asList(new Node[] { Node.getNode(0), Node.getNode(1),
                    Node.getNode(2), Node.getNode(3), Node.getNode(4) });
            if (this.route.isWestbound()) {
                Collections.reverse(nodes);
            }
            Assert.assertEquals(nodes, new LinkedList<Node>(progression.getNodes()));
        }
    }

    @Test
    public void testContains() {
        final Arc arc = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc arc2 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        Route r = this.route;
        Assert.assertFalse("Empty collection shouldn't contain the arc.", r.getProgression()
                .contains(arc));
        r = this.builder.add(arc).build();
        Assert.assertTrue("Collection should now contain the arc.", r.getProgression()
                .contains(arc));
        Assert.assertFalse("Collection should not contain the never-inserted arc.", r
                .getProgression().contains(arc2));
    }

    @Test
    public void testGetNextAndPrevious() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        final Arc firstExtend = this.route.isEastbound() ? a1 : a2;
        final Arc secondExtend = this.route.isEastbound() ? a2 : a1;
        final Builder b = this.builder.add(firstExtend);
        Route r = b.build();
        Assert.assertNull("On a route with single arc, next arc to the first one is null.", r
                .getProgression().getNextArc(firstExtend));
        Assert.assertNull("On a route with single arc, previous arc to the first one is null.", r
                .getProgression().getPreviousArc(firstExtend));
        r = b.add(secondExtend).build();
        Assert.assertNull("On route with two arcs, next arc to the second one is null.", r
                .getProgression().getNextArc(secondExtend));
        Assert.assertSame("On route with two arcs, next arc to the first one is the second.",
                secondExtend, r.getProgression().getNextArc(firstExtend));
        Assert.assertSame("On route with two arcs, previous arc to the second one is the first.",
                firstExtend, r.getProgression().getPreviousArc(secondExtend));
        Assert.assertNull("On route with two arcs, previous arc to the first one is null.", r
                .getProgression().getPreviousArc(firstExtend));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextEmptyRoute() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = this.route;
        r.getProgression().getNextArc(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextInvalid() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = this.builder.add(a1).build();
        r.getProgression().getNextArc(a2);
    }

    @Test
    public void testGetNextNull() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        final Builder b = this.builder.add(a1);
        Route r = b.build();
        Assert.assertSame("On a route with single arc, null next arc is the first one.", r
                .getProgression().getOrigin(), r.getProgression().getNextArc((Arc) null));
        r = b.add(a2).build();
        Assert.assertSame("On a route with two arcs, null next arc is still the first one.", r
                .getProgression().getOrigin(), r.getProgression().getNextArc((Arc) null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextNullEmptyRoute() {
        final Route r = this.route;
        r.getProgression().getNextArc((Arc) null);
    }

    @Test
    public void testGetOriginAndDestination() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(Track.MAIN_0, BigDecimal.ONE, n3, n4);

        Builder b = this.builder.add(a1);
        Route r = b.build();
        Assert.assertSame("With just one arc, initial and terminal arcs should be the same. ", r
                .getProgression().getDestination(), r.getProgression().getOrigin());
        Assert.assertSame("With just one arc, initial and terminal arcs should equal. ", r
                .getProgression().getDestination(), r.getProgression().getOrigin());

        b = b.add(a2);
        r = b.build();
        Assert.assertSame("With two arcs, the one with no incoming connections should be initial.",
                this.route.isEastbound() ? a1 : a2, r.getProgression().getOrigin());
        Assert.assertSame(
                "With two arcs, the one with no outgoing connections should be terminal.",
                this.route.isEastbound() ? a2 : a1, r.getProgression().getDestination());

        r = b.add(a3).build();
        Assert.assertSame(
                "With three arcs, the one with no incoming connections should be initial.",
                this.route.isEastbound() ? a1 : a3, r.getProgression().getOrigin());
        Assert.assertSame(
                "With three arcs, the one with no outgoing connections should be terminal.",
                this.route.isEastbound() ? a3 : a1, r.getProgression().getDestination());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousEmptyRoute() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = this.route;
        r.getProgression().getPreviousArc(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousInvalid() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = this.builder.add(a1).build();
        r.getProgression().getPreviousArc(a2);
    }

    @Test
    public void testGetPreviousNull() {
        // prepare data
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, n2, n3);
        // validate
        final Builder b = this.builder.add(a1);
        Route r = b.build();
        Assert.assertSame("On a route with single arc, null previous arc is the first one.", r
                .getProgression().getDestination(), r.getProgression().getPreviousArc((Arc) null));
        r = b.add(a2).build();
        Assert.assertSame("On a route with two arcs, null previous arc is the last one.", r
                .getProgression().getDestination(), r.getProgression().getPreviousArc((Arc) null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousNullEmptyRoute() {
        final Route r = this.route;
        r.getProgression().getPreviousArc((Arc) null);
    }

    @Test
    public void testGetWaitPointsOnCrossovers() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.CROSSOVER, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(Track.MAIN_0, BigDecimal.ONE, n3, n4);
        final Route r = this.builder.add(a1).add(a2).add(a3).build();
        final Collection<Node> wp = r.getProgression().getWaitPoints();
        Assert.assertEquals("One C means two wait points, start + C.", 2, wp.size());
        Assert.assertTrue("One of the wait points should be the route start.",
                wp.contains(r.getProgression().getOrigin().getOrigin(r)));
        Assert.assertTrue("C waypoint is at the beginning side.", wp.contains(a2.getOrigin(r)));
    }

    @Test
    public void testGetWaitPointsOnMainTracks() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.MAIN_1, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(Track.MAIN_2, BigDecimal.ONE, n3, n4);
        final Route r = this.builder.add(a1).add(a2).add(a3).build();
        final Collection<Node> wp = r.getProgression().getWaitPoints();
        Assert.assertEquals("Only main tracks means just one wait point at the beginning.", 1,
                wp.size());
        Assert.assertTrue("One of the wait points should be the route start.",
                wp.contains(r.getProgression().getOrigin().getOrigin(r)));
    }

    @Test
    public void testGetWaitPointsOnSiding() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(Track.SIDING, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(Track.MAIN_0, BigDecimal.ONE, n3, n4);
        Route r = null;
        if (this.builder.isEastbound()) {
            r = this.builder.add(a1).add(a2).add(a3).build();
        } else {
            r = this.builder.add(a3).add(a2).add(a1).build();
        }
        final Collection<Node> wp = r.getProgression().getWaitPoints();
        Assert.assertEquals("One siding means two wait points, start + siding.", 2, wp.size());
        Assert.assertTrue("One of the wait points should be the route start.",
                wp.contains(r.getProgression().getOrigin().getOrigin(r)));
        Assert.assertTrue("Sidings waypoint is at the end side of the siding.",
                wp.contains(a2.getDestination(r)));
    }

}

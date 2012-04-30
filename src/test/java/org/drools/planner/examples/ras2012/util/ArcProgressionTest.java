package org.drools.planner.examples.ras2012.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;
import org.drools.planner.examples.ras2012.model.original.Track;
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
        final Collection<Object[]> directions = new ArrayList<Object[]>();
        directions.add(new Route[] { new Route(true) });
        directions.add(new Route[] { new Route(false) });
        return directions;
    }

    private final Route route;

    public ArcProgressionTest(final Route r) {
        this.route = r;
    }

    @Test
    public void testCompleteHeadAndTail() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc b = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Arc c = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(2), Node.getNode(3));
        final List<Arc> arcs = Arrays.asList(new Arc[] { a, b, c });
        final ArcProgression p = new ArcProgression(this.route, arcs);

        // testing complete head/tail
        Assert.assertSame("Head ends with last node; result should be full.", p,
                p.head(p.getDestination().getDestination(p)));
        Assert.assertSame("Tail starts with first node; result should be full.", p,
                p.tail(p.getOrigin().getOrigin(p)));
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
        r = r.extend(arc);
        Assert.assertTrue("Collection should now contain the arc.", r.getProgression()
                .contains(arc));
        Assert.assertFalse("Collection should not contain the never-inserted arc.", r
                .getProgression().contains(arc2));
    }

    @Test
    public void testEmptyHeadAndTail() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc b = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Arc c = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(2), Node.getNode(3));
        final List<Arc> arcs = Arrays.asList(new Arc[] { a, b, c });
        final ArcProgression p = new ArcProgression(this.route, arcs);

        // testing empty head/tail
        Assert.assertEquals("Head ends with first node; result should be empty.", 0,
                p.head(p.getOrigin().getOrigin(p)).countArcs());
        Assert.assertEquals("Tail starts with last node; result should be empty.", 0,
                p.tail(p.getDestination().getDestination(p)).countArcs());
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
        Route r = this.route;
        r = r.extend(firstExtend);
        Assert.assertNull("On a route with single arc, next arc to the first one is null.", r
                .getProgression().getNext(firstExtend));
        Assert.assertNull("On a route with single arc, previous arc to the first one is null.", r
                .getProgression().getPrevious(firstExtend));
        r = r.extend(secondExtend);
        Assert.assertNull("On route with two arcs, next arc to the second one is null.", r
                .getProgression().getNext(secondExtend));
        Assert.assertSame("On route with two arcs, next arc to the first one is the second.",
                secondExtend, r.getProgression().getNext(firstExtend));
        Assert.assertSame("On route with two arcs, previous arc to the second one is the first.",
                firstExtend, r.getProgression().getPrevious(secondExtend));
        Assert.assertNull("On route with two arcs, previous arc to the first one is null.", r
                .getProgression().getPrevious(firstExtend));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextEmptyRoute() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Route r = this.route;
        r.getProgression().getNext(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextInvalid() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = this.route.extend(a1);
        r.getProgression().getNext(a2);
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
        Route r = this.route;
        r = r.extend(a1);
        Assert.assertSame("On a route with single arc, null next arc is the first one.", r
                .getProgression().getOrigin(), r.getProgression().getNext(null));
        r = r.extend(a2);
        Assert.assertSame("On a route with two arcs, null next arc is still the first one.", r
                .getProgression().getOrigin(), r.getProgression().getNext(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextNullEmptyRoute() {
        final Route r = this.route;
        r.getProgression().getNext(null);
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

        Route r = this.route.extend(a1);
        Assert.assertSame("With just one arc, initial and terminal arcs should be the same. ", r
                .getProgression().getDestination(), r.getProgression().getOrigin());
        Assert.assertSame("With just one arc, initial and terminal arcs should equal. ", r
                .getProgression().getDestination(), r.getProgression().getOrigin());

        r = r.extend(a2);
        Assert.assertSame("With two arcs, the one with no incoming connections should be initial.",
                this.route.isEastbound() ? a1 : a2, r.getProgression().getOrigin());
        Assert.assertSame(
                "With two arcs, the one with no outgoing connections should be terminal.",
                this.route.isEastbound() ? a2 : a1, r.getProgression().getDestination());

        r = r.extend(a3);
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
        r.getProgression().getPrevious(a1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousInvalid() {
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc a2 = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Route r = this.route.extend(a1);
        r.getProgression().getPrevious(a2);
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
        Route r = this.route;
        r = r.extend(a1);
        Assert.assertSame("On a route with single arc, null previous arc is the first one.", r
                .getProgression().getDestination(), r.getProgression().getPrevious(null));
        r = r.extend(a2);
        Assert.assertSame("On a route with two arcs, null previous arc is the last one.", r
                .getProgression().getDestination(), r.getProgression().getPrevious(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPreviousNullEmptyRoute() {
        final Route r = this.route;
        r.getProgression().getPrevious(null);
    }

    @Test
    public void testGetWaitPointsOnCrossovers() {
        this.testGetWaitPointsOnSwitchesAndCrossovers(Track.CROSSOVER);
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
        final Route r = this.route.extend(a1).extend(a2).extend(a3);
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
        Route r = this.route;
        if (r.isEastbound()) {
            r = r.extend(a1).extend(a2).extend(a3);
        } else {
            r = r.extend(a3).extend(a2).extend(a1);
        }
        final Collection<Node> wp = r.getProgression().getWaitPoints();
        Assert.assertEquals("One siding means two wait points, start + siding.", 2, wp.size());
        Assert.assertTrue("One of the wait points should be the route start.",
                wp.contains(r.getProgression().getOrigin().getOrigin(r)));
        Assert.assertTrue("Sidings waypoint is at the end side of the siding.",
                wp.contains(a2.getDestination(r)));
    }

    @Test
    public void testGetWaitPointsOnSwitches() {
        this.testGetWaitPointsOnSwitchesAndCrossovers(Track.SWITCH);
    }

    private void testGetWaitPointsOnSwitchesAndCrossovers(final Track t) {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final Node n3 = Node.getNode(2);
        final Node n4 = Node.getNode(3);
        final Arc a1 = new Arc(Track.MAIN_0, BigDecimal.ONE, n1, n2);
        final Arc a2 = new Arc(t, BigDecimal.ONE, n2, n3);
        final Arc a3 = new Arc(Track.MAIN_0, BigDecimal.ONE, n3, n4);
        final Route r = this.route.extend(a1).extend(a2).extend(a3);
        final Collection<Node> wp = r.getProgression().getWaitPoints();
        Assert.assertEquals("One SW/C means two wait points, start + SW/C.", 2, wp.size());
        Assert.assertTrue("One of the wait points should be the route start.",
                wp.contains(r.getProgression().getOrigin().getOrigin(r)));
        Assert.assertTrue("SW/C waypoint is at the beginning side.", wp.contains(a2.getOrigin(r)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHeadNullNode() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final ArcProgression p = new ArcProgression(this.route, a);
        Assert.assertEquals("Tail starts with last node; result should be empty.", 0,
                p.head(Node.getNode(2)));
    }

    @Test
    public void testPartialHeadAndTail() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final Arc b = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(1), Node.getNode(2));
        final Arc c = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(2), Node.getNode(3));
        final Arc d = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(3), Node.getNode(4));
        final List<Arc> arcs = Arrays.asList(new Arc[] { a, b, c, d });
        final ArcProgression p = new ArcProgression(this.route, arcs);

        // testing partial head/tail
        Assert.assertEquals("Head ends with second node; result should be one item.",
                p.isEastbound() ? Arrays.asList(new Arc[] { a }) : Arrays.asList(new Arc[] { d }),
                p.head(p.isEastbound() ? Node.getNode(1) : Node.getNode(3)).getArcs());
        Assert.assertEquals("Tail starts with second-to-last node; result should be one item.",
                p.isEastbound() ? Arrays.asList(new Arc[] { d }) : Arrays.asList(new Arc[] { a }),
                p.tail(p.isEastbound() ? Node.getNode(3) : Node.getNode(1)).getArcs());

        Assert.assertEquals(
                "Head ends with second-to-last node; result should be three items.",
                p.isEastbound() ? Arrays.asList(new Arc[] { a, b, c }) : Arrays.asList(new Arc[] {
                        d, c, b }), p.head(p.isEastbound() ? Node.getNode(3) : Node.getNode(1))
                        .getArcs());
        Assert.assertEquals(
                "Tail starts with second node; result should be three items.",
                p.isEastbound() ? Arrays.asList(new Arc[] { b, c, d }) : Arrays.asList(new Arc[] {
                        c, b, a }), p.tail(p.isEastbound() ? Node.getNode(1) : Node.getNode(3))
                        .getArcs());

        Assert.assertEquals(
                "Head ends with middle node; result should be two items.",
                p.isEastbound() ? Arrays.asList(new Arc[] { a, b }) : Arrays.asList(new Arc[] { d,
                        c }), p.head(Node.getNode(2)).getArcs());
        Assert.assertEquals(
                "Tail starts with middle node; result should be two items.",
                p.isEastbound() ? Arrays.asList(new Arc[] { c, d }) : Arrays.asList(new Arc[] { b,
                        a }), p.tail(Node.getNode(2)).getArcs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTailNullNode() {
        final Arc a = new Arc(Track.MAIN_0, BigDecimal.ONE, Node.getNode(0), Node.getNode(1));
        final ArcProgression p = new ArcProgression(this.route, a);
        Assert.assertEquals("Tail starts with last node; result should be empty.", 0,
                p.tail(Node.getNode(2)));
    }
}

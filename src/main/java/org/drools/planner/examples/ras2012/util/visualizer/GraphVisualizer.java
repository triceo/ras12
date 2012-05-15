package org.drools.planner.examples.ras2012.util.visualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;

public class GraphVisualizer {

    private static class ArcLabeller implements Transformer<Arc, String> {

        @Override
        public String transform(final Arc input) {
            return input.getTrack().getSymbol() + "" + input.getLength();
        }

    }

    private static final class EdgePainter implements Transformer<Arc, Stroke> {

        private final Route route;

        public EdgePainter(final Route r) {
            this.route = r;
        }

        @Override
        public Stroke transform(final Arc input) {
            if (this.route.getProgression().isPreferred(input)) {
                return new BasicStroke();
            } else {
                return RenderContext.DOTTED;
            }
        }
    }

    private static class NodeLabeller implements Transformer<Node, String> {

        @Override
        public String transform(final Node input) {
            return String.valueOf(input.getId());
        }

    }

    private static final class NodePainter implements Transformer<Node, Paint> {

        private final Route route;

        public NodePainter(final Route r) {
            this.route = r;
        }

        @Override
        public Paint transform(final Node input) {
            if (input == this.route.getProgression().getOrigin().getOrigin(this.route)) {
                return Color.GREEN;
            } else if (input == this.route.getProgression().getDestination()
                    .getDestination(this.route)) {
                return Color.RED;
            } else if (this.route.getProgression().getWaitPoints().contains(input)) {
                return Color.BLUE;
            } else {
                return Color.GRAY;
            }
        }
    }

    private static final int      GRAPH_WIDTH  = 1920;

    private static final int      GRAPH_HEIGHT = 1080;

    private final Collection<Arc> edges;
    private final Route           route;

    private static final Lock     l            = new ReentrantLock();

    public GraphVisualizer(final Collection<Arc> edges) {
        this(edges, null);
    }

    protected GraphVisualizer(final Collection<Arc> edges, final Route r) {
        this.edges = edges;
        this.route = r;
    }

    private Graph<Node, Arc> formGraph() {
        Graph<Node, Arc> g = null;
        if (this.route != null) {
            g = new DirectedOrderedSparseMultigraph<Node, Arc>();
        } else {
            g = new UndirectedOrderedSparseMultigraph<Node, Arc>();
        }
        final boolean isGraphDirected = g instanceof DirectedGraph;
        for (final Arc a : this.edges) {
            final Node origin = isGraphDirected ? a.getOrigin(this.route) : a.getOrigin(new Route(
                    false));
            final Node destination = isGraphDirected ? a.getDestination(this.route) : a
                    .getDestination(new Route(false));
            g.addVertex(origin);
            g.addVertex(destination);
            g.addEdge(a, origin, destination);
        }
        return g;
    }

    protected Layout<Node, Arc> getLayout() {
        final Layout<Node, Arc> layout = new ISOMLayout<Node, Arc>(this.formGraph());
        layout.setSize(new Dimension(GraphVisualizer.GRAPH_WIDTH, GraphVisualizer.GRAPH_HEIGHT));
        return layout;
    }

    protected VisualizationImageServer<Node, Arc> getServer() {
        final Layout<Node, Arc> layout = this.getLayout();
        final VisualizationImageServer<Node, Arc> server = new VisualizationImageServer<Node, Arc>(
                layout, layout.getSize());
        if (layout.getGraph() instanceof DirectedGraph) {
            server.getRenderContext().setVertexFillPaintTransformer(new NodePainter(this.route));
            server.getRenderContext().setEdgeStrokeTransformer(new EdgePainter(this.route));
        }
        server.getRenderContext().setLabelOffset(30);
        server.getRenderContext().setEdgeLabelTransformer(new ArcLabeller());
        server.getRenderContext().setVertexLabelTransformer(new NodeLabeller());
        return server;
    }

    public void visualize(final OutputStream visualize) throws IOException {
        final VisualizationImageServer<Node, Arc> server = this.getServer();
        Image i = null;
        GraphVisualizer.l.lock();
        try {
            /*
             * the call to getImage() causes trouble when running in multiple threads; keep other threads out using a lock.
             */
            i = server.getImage(new Point2D.Double(GraphVisualizer.GRAPH_WIDTH / 2,
                    GraphVisualizer.GRAPH_HEIGHT / 2), server.getSize());
        } finally {
            GraphVisualizer.l.unlock();
        }
        final BufferedImage bi = new BufferedImage(GraphVisualizer.GRAPH_WIDTH,
                GraphVisualizer.GRAPH_HEIGHT, BufferedImage.TYPE_INT_RGB);
        bi.createGraphics().drawImage(i, null, null);
        ImageIO.write(bi, "png", visualize);
    }
}

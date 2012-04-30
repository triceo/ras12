package org.drools.planner.examples.ras2012.util;

import java.awt.Dimension;
import java.awt.Image;
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
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;

public class GraphVisualizer {

    private static class ArcLabeller implements Transformer<Arc, String> {

        private String getTrackId(final Arc input) {
            switch (input.getTrack()) {
                case MAIN_0:
                    return "M0";
                case MAIN_1:
                    return "M1";
                case MAIN_2:
                    return "M2";
                case SWITCH:
                    return "SW";
                case SIDING:
                    return "S";
                case CROSSOVER:
                    return "C";
                default:
                    throw new IllegalArgumentException("Unknown track type: "
                            + input.getTrack());
            }
        }

        @Override
        public String transform(final Arc input) {
            return input.getLengthInMiles() + this.getTrackId(input);
        }

    }

    private static class NodeLabeller implements Transformer<Node, String> {

        @Override
        public String transform(final Node input) {
            return String.valueOf(input.getId());
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
        for (final Arc a : this.edges) {
            final Node routelessOrigin = a.getOrigin(new Route(false));
            final Node routelessDestination = a.getDestination(new Route(false));
            g.addVertex(routelessOrigin);
            g.addVertex(routelessDestination);
            if (this.route == null) {
                g.addEdge(a, routelessOrigin, routelessDestination);
            } else {
                g.addEdge(a, a.getOrigin(this.route), a.getDestination(this.route));
            }
        }
        return g;
    }

    protected Layout<Node, Arc> getLayout() {
        final Layout<Node, Arc> layout = new ISOMLayout<Node, Arc>(this.formGraph());
        layout.setSize(new Dimension(GraphVisualizer.GRAPH_WIDTH, GraphVisualizer.GRAPH_HEIGHT));
        return layout;
    }

    protected VisualizationImageServer<Node, Arc> getServer() {
        final VisualizationImageServer<Node, Arc> server = new VisualizationImageServer<Node, Arc>(
                this.getLayout(), this.getLayout().getSize());
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

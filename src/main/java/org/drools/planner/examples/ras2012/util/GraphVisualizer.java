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
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;

public class GraphVisualizer {

    private static class ArcLabeller implements Transformer<Arc, String> {

        private String getTrackId(final Arc input) {
            switch (input.getTrackType()) {
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
                            + input.getTrackType());
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

    private static final int       GRAPH_WIDTH  = 1920;

    private static final int       GRAPH_HEIGHT = 1080;

    private final Collection<Node> nodes;
    private final Collection<Arc>  edges;

    public GraphVisualizer(final Collection<Node> nodes, final Collection<Arc> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    private UndirectedGraph<Node, Arc> formGraph() {
        final UndirectedGraph<Node, Arc> g = new UndirectedOrderedSparseMultigraph<Node, Arc>();
        for (final Node n : this.nodes) {
            g.addVertex(n);
        }
        for (final Arc a : this.edges) {
            g.addEdge(a, a.getWestNode(), a.getEastNode());
        }
        return g;
    }

    private static final Lock l = new ReentrantLock();

    public void visualize(final OutputStream visualize) throws IOException {
        final Layout<Node, Arc> layout = new ISOMLayout<>(this.formGraph());
        layout.setSize(new Dimension(GraphVisualizer.GRAPH_WIDTH, GraphVisualizer.GRAPH_HEIGHT));
        final VisualizationImageServer<Node, Arc> server = new VisualizationImageServer<>(layout,
                layout.getSize());
        server.getRenderContext().setLabelOffset(30);
        server.getRenderContext().setEdgeLabelTransformer(new ArcLabeller());
        server.getRenderContext().setVertexLabelTransformer(new NodeLabeller());
        Image i = null;
        l.lock();
        try {
            /*
             * the call to getImage() causes trouble when running in multiple threads; keep other threads out using a lock.
             */
            i = server.getImage(new Point2D.Double(GraphVisualizer.GRAPH_WIDTH / 2,
                    GraphVisualizer.GRAPH_HEIGHT / 2), layout.getSize());
        } finally {
            l.unlock();
        }
        final BufferedImage bi = new BufferedImage(GraphVisualizer.GRAPH_WIDTH,
                GraphVisualizer.GRAPH_HEIGHT, BufferedImage.TYPE_INT_RGB);
        bi.createGraphics().drawImage(i, null, null);
        ImageIO.write(bi, "png", visualize);
    }
}

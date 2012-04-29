package org.drools.planner.examples.ras2012.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.original.Arc;
import org.drools.planner.examples.ras2012.model.original.Node;

public class RouteVisualizer extends GraphVisualizer {

    private static final class EdgePainter implements Transformer<Arc, Stroke> {

        private final Route route;

        public EdgePainter(final Route r) {
            this.route = r;
        }

        @Override
        public Stroke transform(final Arc input) {
            if (this.route.isArcPreferred(input)) {
                return new BasicStroke();
            } else {
                return RenderContext.DOTTED;
            }
        }
    }

    private static final class NodePainter implements Transformer<Node, Paint> {

        private final Route route;

        public NodePainter(final Route r) {
            this.route = r;
        }

        @Override
        public Paint transform(final Node input) {
            if (input == this.route.getOrigin().getOrigin(this.route)) {
                return Color.GREEN;
            } else if (input == this.route.getDestination().getDestination(this.route)) {
                return Color.RED;
            } else if (this.route.getWaitPoints().contains(input)) {
                return Color.BLUE;
            } else {
                return Color.GRAY;
            }
        }
    }

    private final Route route;

    public RouteVisualizer(final Route r) {
        super(r.getArcs(), r);
        this.route = r;
    }

    @Override
    protected VisualizationImageServer<Node, Arc> getServer() {
        final VisualizationImageServer<Node, Arc> server = super.getServer();
        server.getRenderContext().setVertexFillPaintTransformer(new NodePainter(this.route));
        server.getRenderContext().setEdgeStrokeTransformer(new EdgePainter(this.route));
        return server;
    }
}

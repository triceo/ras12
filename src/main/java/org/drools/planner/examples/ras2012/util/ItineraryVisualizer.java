package org.drools.planner.examples.ras2012.util;

import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.Node;

public class ItineraryVisualizer extends RouteVisualizer {

    private static final class NodeLabeller implements Transformer<Node, String> {

        private final Itinerary itinerary;

        public NodeLabeller(final Itinerary i) {
            this.itinerary = i;
        }

        @Override
        public String transform(final Node input) {
            if (this.itinerary.getAllWaitTimes().containsKey(input)) {
                return input.getId() + " +" + this.itinerary.getWaitTime(input);
            } else {
                return String.valueOf(input.getId());
            }
        }

    }

    private final Itinerary itinerary;

    public ItineraryVisualizer(final Itinerary i) {
        super(i.getRoute());
        this.itinerary = i;
    }

    @Override
    protected VisualizationImageServer<Node, Arc> getServer() {
        final VisualizationImageServer<Node, Arc> server = super.getServer();
        server.getRenderContext().setVertexLabelTransformer(new NodeLabeller(this.itinerary));
        return server;
    }

}

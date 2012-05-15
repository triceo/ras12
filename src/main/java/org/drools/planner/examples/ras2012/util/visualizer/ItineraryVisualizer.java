package org.drools.planner.examples.ras2012.util.visualizer;

import java.awt.Color;
import java.awt.Paint;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import edu.uci.ics.jung.visualization.VisualizationImageServer;
import org.apache.commons.collections15.Transformer;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.util.model.OccupationTracker;

public class ItineraryVisualizer extends RouteVisualizer {

    private static final class EdgePainter implements Transformer<Arc, Paint> {

        private final OccupationTracker arcs;

        public EdgePainter(final OccupationTracker occupiedArcs) {
            this.arcs = occupiedArcs;
        }

        @Override
        public Paint transform(final Arc input) {
            if (this.arcs.getIncludedArcs().contains(input)) {
                return Color.RED;
            } else {
                return Color.BLACK;
            }
        }

    }

    private static final class NodeLabeller implements Transformer<Node, String> {

        private final Itinerary itinerary;

        public NodeLabeller(final Itinerary i) {
            this.itinerary = i;
        }

        @Override
        public String transform(final Node input) {
            final SortedMap<Long, Node> schedule = this.itinerary.getSchedule();
            if (schedule.containsValue(input)) {
                for (final SortedMap.Entry<Long, Node> entry : schedule.entrySet()) {
                    if (entry.getValue() == input) {
                        return input.getId() + "@"
                                + TimeUnit.MILLISECONDS.toMinutes(entry.getKey());
                    }
                }
                return null;
            } else {
                return String.valueOf(input.getId());
            }
        }

    }

    private final Itinerary itinerary;
    private final long      time;

    public ItineraryVisualizer(final Itinerary i) {
        this(i, -1);
    }

    public ItineraryVisualizer(final Itinerary i, final long time) {
        this(i, time, TimeUnit.MINUTES);
    }

    public ItineraryVisualizer(final Itinerary i, final long time, final TimeUnit unit) {
        super(i.getRoute());
        this.itinerary = i;
        this.time = TimeUnit.MILLISECONDS.convert(time, unit);
    }

    @Override
    protected VisualizationImageServer<Node, Arc> getServer() {
        final VisualizationImageServer<Node, Arc> server = super.getServer();
        server.getRenderContext().setVertexLabelTransformer(new NodeLabeller(this.itinerary));
        if (this.time >= 0) {
            server.getRenderContext().setEdgeDrawPaintTransformer(
                    new EdgePainter(this.itinerary.getOccupiedArcs(this.time)));
        }
        return server;
    }

}

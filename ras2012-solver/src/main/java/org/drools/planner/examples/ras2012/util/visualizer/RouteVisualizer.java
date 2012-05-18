package org.drools.planner.examples.ras2012.util.visualizer;

import org.drools.planner.examples.ras2012.model.Route;

public class RouteVisualizer extends GraphVisualizer {

    public RouteVisualizer(final Route r) {
        super(r.getProgression().getArcs(), r);
    }

}

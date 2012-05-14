package org.drools.planner.examples.ras2012;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.drools.planner.examples.ras2012.util.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Visualizable {

    private static final Logger logger = LoggerFactory.getLogger(Visualizable.class);

    /**
     * Visualize the resource and write the results to the file.
     * 
     * @param target Target for writing the visualization to.
     * @return True if the operation was a success, false otherwise.
     */
    public abstract boolean visualize(File target);

    protected boolean visualize(final GraphVisualizer visualizer, final File target) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            Visualizable.logger.info("Visualizing itinerary: " + this);
            visualizer.visualize(os);
            Visualizable.logger.info("Visualization finished: " + this);
            return true;
        } catch (final Exception ex) {
            Visualizable.logger.error("Visualizing " + this + " failed.", ex);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (final IOException e) {
                    // nothing to do here
                }
            }
        }
    }

}

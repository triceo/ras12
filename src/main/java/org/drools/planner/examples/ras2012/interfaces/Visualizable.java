package org.drools.planner.examples.ras2012.interfaces;

import java.io.IOException;
import java.io.OutputStream;

public interface Visualizable {

    /**
     * Visualize the resource and write the results to the output stream.
     * 
     * @param stream Target for writing the visualization to.
     */
    public void visualize(OutputStream stream) throws IOException;

}

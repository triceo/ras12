package org.drools.planner.examples.ras2012.interfaces;

import java.io.File;

public interface Visualizable {

    /**
     * Visualize the resource and write the results to the file.
     * 
     * @param target Target for writing the visualization to.
     * @return True if the operation was a success, false otherwise.
     */
    public boolean visualize(File target);

}

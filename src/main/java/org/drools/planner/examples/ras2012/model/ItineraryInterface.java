package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.SortedMap;

public interface ItineraryInterface {

    /**
     * Count how many times a train will have to stop on its way to the destination.
     * 
     * @return Number of stops.
     */
    public int countHalts();

    /**
     * Get the arc that the train is occupying at a given point in time.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return The arc where the head of the locomotive is located.
     */
    public Arc getCurrentArc(BigDecimal time);

    /**
     * Return length of track travelled from the beginning of time to the given point in time.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return Value in miles. When the train is already in its destination, it returns the length of the route.
     */
    public BigDecimal getDistanceTravelled(BigDecimal time);

    /**
     * Get every arc that the train is occupying at a given length of time. Arc is either occupied or free, there is no concept
     * of partially-occupied.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return Currently occupied arcs, each once.
     */
    public Collection<Arc> getCurrentlyOccupiedArcs(BigDecimal time);

    /**
     * Get the first next node that the train is going to arrive at, given the current point in time.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return The next node.
     */
    public Node getNextNodeToReach(BigDecimal time);

    /**
     * Retrieve the schedule for the given train on the given route. The schedule takes into account all the maintenance windows
     * and halt points.
     * 
     * @return Map, where the key is the node and the value is the time that the train arrived at the node.
     */
    public SortedMap<Node, BigDecimal> getSchedule();

    /**
     * Specify that a train is supposed to wait when it arrives at a given node.
     * 
     * @param wait How long to wait for.
     * @param node What node to wait at.
     * @return The wait time that the node had previously, or null.
     */
    public WaitTime setWaitTime(WaitTime wait, Node node);

    /**
     * Specify that a train shouldn't wait at a given node.
     * 
     * @param n The node that the wait time should be removed from.
     * @return The previous wait time for the node, or null.
     */
    public WaitTime removeWaitTime(Node n);
}

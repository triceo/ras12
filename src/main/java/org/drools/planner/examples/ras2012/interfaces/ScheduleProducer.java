package org.drools.planner.examples.ras2012.interfaces;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;

public interface ScheduleProducer {

    /**
     * Default scale to use for BigDecimals returned by the methods of this interface.
     */
    public static final int BIGDECIMAL_SCALE    = 5;
    /**
     * Default rounding to use for BigDecimals returned by the methods of this interface.
     */
    public static final int BIGDECIMAL_ROUNDING = BigDecimal.ROUND_DOWN;

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
     * @return The arc where the head of the locomotive is located. When a train isn't en route yet or already, return null.
     */
    public Arc getCurrentArc(BigDecimal time);

    /**
     * Get every arc that the train is occupying at a given length of time. Arc is either occupied or free, there is no concept
     * of partially-occupied.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return Currently occupied arcs, each once.
     */
    public Collection<Arc> getCurrentlyOccupiedArcs(BigDecimal time);

    /**
     * Return length of track travelled from the beginning of time to the given point in time.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return Value in miles, to @see{Itinerary.BIGDECIMAL_SCALE} decimal digits, rounding @see{Itinerary.BIGDECIMAL_ROUNDING}.
     *         When the train is already in its destination, it returns the length of the route.
     */
    public BigDecimal getDistanceTravelled(BigDecimal time);

    /**
     * Retrieve the route that this itinerary relates to.
     * 
     * @return The route, never null.
     */
    public Route getRoute();

    /**
     * Retrieve the schedule for the given train on the given route. The schedule takes into account all the maintenance windows
     * and halt points.
     * 
     * @return Map, where the key is the time and the value is the node reached at that time.
     */
    public SortedMap<BigDecimal, Node> getSchedule();

    /**
     * Retrieve the difference between the train's schedule and the reality.
     * 
     * @return Key is the time of arrival at the SA checkpoint, value is the difference. Positive when there's been a delay,
     *         negative when the train is ahead of schedule.
     */
    public Map<BigDecimal, BigDecimal> getScheduleAdherenceStatus();

    /**
     * Retrieve the train that this itinerary relates to.
     * 
     * @return The train, never null.
     */
    public Train getTrain();

    /**
     * Retrieve the wait time for the given node.
     * 
     * @param n The node that the wait time should be looked at for.
     * @return The wait time.
     */
    public WaitTime getWaitTime(Node n);

    /**
     * Retrieve the difference between the train's want time and the actual time of arrival.
     * 
     * @return Only contains one entry. Key is the tim of arrival at the destination node, value is the difference. Positive
     *         when there's been a delay, negative when the train is ahead of schedule.
     */
    public Map<BigDecimal, BigDecimal> getWantTimeDifference();

    /**
     * Remove all previously set wait times.
     */
    public void removeAllWaitTimes();

    /**
     * Specify that a train shouldn't wait at a given node.
     * 
     * @param n The node that the wait time should be removed from.
     * @return The previous wait time for the node, or null.
     */
    public WaitTime removeWaitTime(Node n);

    /**
     * Specify that a train is supposed to wait when it arrives at a given node.
     * 
     * @param wait How long to wait for.
     * @param node What node to wait at.
     * @return The wait time that the node had previously, or null.
     */
    public WaitTime setWaitTime(WaitTime wait, Node node);
}

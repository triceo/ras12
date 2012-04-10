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

// FIXME make visualizable
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
     * Get every arc that the train is occupying at a given length of time. Arc is either occupied or free, there is no concept
     * of partially-occupied.
     * 
     * @param time Number in minutes, specifying the time since the beginning of world.
     * @return Currently occupied arcs, each once.
     */
    public Collection<Arc> getCurrentlyOccupiedArcs(BigDecimal time);

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
     *         negative when the train is ahead of schedule. Both times are in milliseconds.
     */
    public Map<Long, Long> getScheduleAdherenceStatus();

    /**
     * Retrieve the time in minutes that the train has spent on unpreferred tracks.
     * 
     * @param time Time in minutes.
     * @return Number of milliseconds spent on unpreferred tracks.
     */
    public long getTimeSpentOnUnpreferredTracks(BigDecimal time);

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
     * @return Only contains one entry. Key is the time of arrival at the destination node in milliseconds, value is the
     *         difference in milliseconds. Positive when there's been a delay, negative when the train is ahead of schedule.
     */
    public Map<Long, Long> getWantTimeDifference();

    /**
     * Remove all previously set wait times.
     */
    public void removeAllWaitTimes();

    /**
     * Retrieve all the existing wait times.
     */
    public Map<Node, WaitTime> getAllWaitTimes();

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

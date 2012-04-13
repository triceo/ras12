package org.drools.planner.examples.ras2012.interfaces;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.Route;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.model.WaitTime;

public interface ScheduleProducer extends Visualizable {

    /**
     * Retrieve all the existing wait times.
     */
    public Map<Node, WaitTime> getAllWaitTimes();

    /**
     * Get every arc that the train is occupying at a given length of time. Arc is either occupied or free, there is no concept
     * of partially-occupied.
     * 
     * @param time Number in milliseconds, specifying the time since the beginning of world.
     * @return Currently occupied arcs, each once.
     */
    public Collection<Arc> getCurrentlyOccupiedArcs(long time);

    /**
     * Retrieve delays (WaitTimes + possible stops for MOWs) at specific nodes.
     * 
     * @return Map, where the key is the node and the value is delay in milliseconds caused by that node.
     */
    public Map<Node, Long> getDelays();

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
     * @return Map, where the key is the time (in milliseconds) and the value is the node reached at that time.
     */
    public SortedMap<Long, Node> getSchedule();

    /**
     * Retrieve the difference between the train's schedule and the reality.
     * 
     * @return Key is the time of arrival at the SA checkpoint, value is the difference. Positive when there's been a delay,
     *         negative when the train is ahead of schedule. Both times are in milliseconds.
     */
    public Map<Long, Long> getScheduleAdherenceStatus();

    /**
     * Retrieve the schedule for the given train on the given route. The schedule takes into account all the maintenance windows
     * and halt points.
     * 
     * @return Map, where the key is the time (in milliseconds) and the value is the arc reached at that time.
     */
    public SortedMap<Long, Arc> getScheduleWithArcs();

    /**
     * Retrieve the time that the train has spent on unpreferred tracks.
     * 
     * @param time Time in milliseconds.
     * @return Number of milliseconds spent on unpreferred tracks.
     */
    public long getTimeSpentOnUnpreferredTracks(long time);

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
     * Whether or not a given node is on the route, ie. in between train origin and destination (incl.).
     * 
     * @param n Node to ask for.
     * @return
     */
    public boolean isNodeOnRoute(Node n);

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

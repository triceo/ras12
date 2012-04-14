package org.drools.planner.examples.ras2012.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;
import org.drools.planner.examples.ras2012.model.Arc.TrackType;
import org.drools.planner.examples.ras2012.model.Train.TrainType;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TrainTest {

    @BeforeClass
    public static void setSpeeds() {
        TrackType.setSpeed(TrackType.MAIN_0, 100, 50);
        TrackType.setSpeed(TrackType.MAIN_1, 85, 75);
        TrackType.setSpeed(TrackType.MAIN_2, 110, 95);
        TrackType.setSpeed(TrackType.SIDING, 25);
        TrackType.setSpeed(TrackType.SWITCH, 35);
        TrackType.setSpeed(TrackType.CROSSOVER, 45);
    }

    @Test
    @Ignore
    public void testConstructor() {
        // TODO
    }

    @Test
    public void testDirection() {
        final Train east = new Train("EB", BigDecimal.ONE, BigDecimal.ONE, 90, Node.getNode(0),
                Node.getNode(1), 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, false);
        Assert.assertTrue(east.isEastbound());
        Assert.assertFalse(east.isWestbound());
        final Train west = new Train("WB", BigDecimal.ONE, BigDecimal.ONE, 90, Node.getNode(0),
                Node.getNode(1), 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, true);
        Assert.assertTrue(west.isWestbound());
        Assert.assertFalse(west.isEastbound());
    }

    @Test
    @Ignore
    public void testEqualsObject() {
        // TODO
    }

    @Test
    public void testGetArcTravellingTimeInMilliseconds() {
        final Node n1 = Node.getNode(0);
        final Node n2 = Node.getNode(1);
        final BigDecimal length = new BigDecimal("1.5");
        // prepare arcs, all of the same length
        final Arc mainArc0 = new Arc(TrackType.MAIN_0, length, n1, n2);
        final Arc mainArc1 = new Arc(TrackType.MAIN_0, length, n1, n2);
        final Arc mainArc2 = new Arc(TrackType.MAIN_0, length, n1, n2);
        final Arc sidingArc = new Arc(TrackType.SIDING, length, n1, n2);
        final Arc switchArc = new Arc(TrackType.SWITCH, length, n1, n2);
        final Arc crossoverArc = new Arc(TrackType.CROSSOVER, length, n1, n2);
        final Arc[] arcs = new Arc[] { mainArc0, mainArc1, mainArc2, sidingArc, switchArc,
                crossoverArc };
        // now prepare various trains
        final Random rand = new Random(0); // speed multipliers will be random, but with a fixed seed, so repeatable
        final Train aTrainWest = new Train("A1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train bTrainWest = new Train("B1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train cTrainWest = new Train("C1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train dTrainWest = new Train("D1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train eTrainWest = new Train("E1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train fTrainWest = new Train("F1", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n2, n1, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, true);
        final Train aTrainEast = new Train("A2", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train bTrainEast = new Train("B2", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train cTrainEast = new Train("C2", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train dTrainEast = new Train("D2", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train eTrainEast = new Train("E2", BigDecimal.ONE, BigDecimal.valueOf(rand
                .nextDouble()), 90, n1, n2, 0, 0, 0,
                Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train fTrainEast = new Train("F2", BigDecimal.ONE, BigDecimal.ONE, 90, n1, n2, 0, 0,
                0, Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
        final Train[] trains = new Train[] { aTrainWest, bTrainWest, cTrainWest, dTrainWest,
                eTrainWest, fTrainWest, aTrainEast, bTrainEast, cTrainEast, dTrainEast, eTrainEast,
                fTrainEast };
        // and now test all against all
        for (final Arc a : arcs) {
            for (final Train t : trains) {
                final BigDecimal distanceInMiles = a.getLengthInMiles();
                final int trainSpeedInMph = t.getMaximumSpeed(a.getTrackType());
                final BigDecimal timeInHours = distanceInMiles.divide(
                        BigDecimal.valueOf(trainSpeedInMph), 10, BigDecimal.ROUND_HALF_EVEN);
                final BigDecimal timeInMilliseconds = timeInHours.multiply(BigDecimal.valueOf(60))
                        .multiply(BigDecimal.valueOf(60)).multiply(BigDecimal.valueOf(1000));
                final long result = timeInMilliseconds.longValue();
                Assert.assertEquals(t + " didn't travel " + a + " in expected time.", result,
                        t.getArcTravellingTimeInMilliseconds(a));
            }
        }
    }

    @Test
    @Ignore
    public void testGetMaximumSpeed() {
        // TODO
    }

    @Test
    public void testGetType() {
        final Map<String, TrainType> types = new HashMap<String, TrainType>();
        types.put("A", TrainType.A);
        types.put("B", TrainType.B);
        types.put("C", TrainType.C);
        types.put("D", TrainType.D);
        types.put("E", TrainType.E);
        types.put("F", TrainType.F);
        final Random rand = new Random();
        for (final Map.Entry<String, TrainType> e : types.entrySet()) {
            final String name = e.getKey() + rand.nextInt();
            final Train t = new Train(name, BigDecimal.ONE, BigDecimal.ONE, 90, Node.getNode(0),
                    Node.getNode(1), 0, 1, 0,
                    Collections.<ScheduleAdherenceRequirement> emptyList(), true, false);
            Assert.assertEquals("Train doesn't have the proper type.", e.getValue(), t.getType());
        }
    }

    @Test
    public void testIsHeavy() {
        final Train heavy = new Train("EB", BigDecimal.ONE, BigDecimal.ONE, 110, Node.getNode(0),
                Node.getNode(1), 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, false);
        Assert.assertTrue(heavy.isHeavy());
        final Train light = new Train("EB", BigDecimal.ONE, BigDecimal.ONE, 90, Node.getNode(0),
                Node.getNode(1), 0, 1, 0, Collections.<ScheduleAdherenceRequirement> emptyList(),
                true, true);
        Assert.assertFalse(light.isHeavy());
    }

}

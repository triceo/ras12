package org.drools.planner.examples.ras2012.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import org.drools.planner.examples.ras2012.RAS2012ScoreCalculator;
import org.drools.planner.examples.ras2012.RAS2012Solution;
import org.drools.planner.examples.ras2012.model.Arc;
import org.drools.planner.examples.ras2012.model.Itinerary;
import org.drools.planner.examples.ras2012.model.MaintenanceWindow;
import org.drools.planner.examples.ras2012.model.Node;
import org.drools.planner.examples.ras2012.model.ScheduleAdherenceRequirement;
import org.drools.planner.examples.ras2012.model.Track;
import org.drools.planner.examples.ras2012.model.Train;
import org.drools.planner.examples.ras2012.parser.DataSetParser;
import org.drools.planner.examples.ras2012.parser.DataSetParser.ParsedTrain;
import org.drools.planner.examples.ras2012.parser.ParseException;
import org.drools.planner.examples.ras2012.parser.Token;
import org.drools.planner.examples.ras2012.util.model.Territory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolutionIO {

    private final Configuration freemarker;

    private Map<Integer, Node>  nodes;

    private static final Logger logger = LoggerFactory.getLogger(SolutionIO.class);

    private static BigDecimal convertMillisToSeconds(final long time) {
        return BigDecimal
                .valueOf(time)
                .divide(BigDecimal.valueOf(1000), Converter.BIGDECIMAL_SCALE,
                        Converter.BIGDECIMAL_ROUNDING).setScale(3);
    }

    private static Track getArcType(final Token t) {
        final String value = SolutionIO.tokenToString(t);
        if (value.equals("0")) {
            return Track.MAIN_0;
        } else if (value.equals("1")) {
            return Track.MAIN_1;
        } else if (value.equals("2")) {
            return Track.MAIN_2;
        } else if (value.equals("SW")) {
            return Track.SWITCH;
        } else if (value.equals("S")) {
            return Track.SIDING;
        } else if (value.equals("C")) {
            return Track.CROSSOVER;
        } else {
            throw new IllegalArgumentException("Invalid value for track type: " + value);
        }
    }

    private static BigDecimal tokenToBigDecimal(final Token t) {
        return new BigDecimal(SolutionIO.tokenToString(t));
    }

    private static boolean tokenToBoolean(final Token t) {
        return Boolean.valueOf(SolutionIO.tokenToString(t));
    }

    private static Integer tokenToInteger(final Token t) {
        return Integer.valueOf(SolutionIO.tokenToString(t));
    }

    private static String tokenToString(final Token t) {
        return t.toString();
    }

    private static final void writeTrain(final Train t, final RAS2012Solution solution,
            final BufferedWriter w) throws IOException {
        w.write("\t\t<train id='" + t.getName() + "'>");
        w.newLine();
        w.write("\t\t\t<movements>");
        w.newLine();
        for (final SortedMap.Entry<Long, Arc> entry : solution.getAssignment(t).getItinerary()
                .getScheduleWithArcs().entrySet()) {
            final Arc arc = entry.getValue();
            if (entry.getKey() >= solution.getPlanningHorizon(TimeUnit.MILLISECONDS)) {
                continue;
            }
            final BigDecimal timeInSeconds = SolutionIO.convertMillisToSeconds(entry.getKey() - 1);
            if (arc != null) {
                final BigDecimal distance = arc.getLength().add(t.getLength());
                final long travellingTime = Converter.getTimeFromSpeedAndDistance(
                        t.getMaximumSpeed(arc.getTrack()), distance);
                final BigDecimal leaveTime = SolutionIO.convertMillisToSeconds(travellingTime).add(
                        timeInSeconds);
                if (leaveTime.intValue() > solution.getPlanningHorizon(TimeUnit.MILLISECONDS)) {
                    continue;
                }
                w.write("\t\t\t\t<movement arc='(" + arc.getOrigin(t).getId() + ","
                        + arc.getDestination(t).getId() + ")' entry='" + timeInSeconds + "' exit='"
                        + leaveTime + "' />");
                w.newLine();
            } else {
                w.write("\t\t\t\t<destination entry='" + timeInSeconds + "' />");
                w.newLine();
            }
        }
        w.write("\t\t\t</movements>");
        w.newLine();
        w.write("\t\t</train>");
        w.newLine();
    }

    public SolutionIO() {
        this.freemarker = new Configuration();
        this.freemarker.setClassForTemplateLoading(SolutionIO.class, "");
        this.freemarker.setObjectWrapper(new DefaultObjectWrapper());
        this.freemarker.setLocale(Locale.US);
        this.freemarker.setNumberFormat("computer");
    }

    private RAS2012Solution createSolution(final DataSetParser p) {
        // retrieve speeds for different track types
        final int eastboundSpeed = SolutionIO.tokenToInteger(p.getSpeedEastbound());
        final int westboundSpeed = SolutionIO.tokenToInteger(p.getSpeedWestbound());
        final int sidingsSpeed = SolutionIO.tokenToInteger(p.getSpeedSidings());
        final int crossoverSpeed = SolutionIO.tokenToInteger(p.getSpeedCrossovers());
        // set speeds for different track types
        for (final Track t : Track.values()) {
            if (t.isMainTrack()) {
                Track.setSpeed(t, eastboundSpeed, westboundSpeed);
            } else if (t == Track.SIDING) {
                Track.setSpeed(t, sidingsSpeed);
            } else {
                Track.setSpeed(t, crossoverSpeed);
            }
        }
        final String name = SolutionIO.tokenToString(p.getName());
        final Collection<Arc> arcs = this.initArcs(p);
        final Collection<MaintenanceWindow> mows = this.initMOW(p);
        final Collection<Train> trains = this.initTrains(name, p);
        return new RAS2012Solution(name, new Territory(this.nodes.values(), arcs), mows, trains);
    }

    private Collection<Arc> initArcs(final DataSetParser p) {
        final List<Token> trackTypes = p.getTracks();
        final List<List<Token>> arcs = p.getArcs();
        final List<Token> trackLengths = p.getLengths();
        // first validate that there is proper amount of data in all structures
        final int numberOfItems = arcs.size();
        if (trackLengths.size() != numberOfItems) {
            throw new IllegalStateException("Arc lengths do not correspond to their number.");
        }
        if (trackTypes.size() != numberOfItems) {
            throw new IllegalStateException("Arc types do not correspond to their number.");
        }
        // now start processing
        final List<Arc> newArcs = new ArrayList<Arc>();
        final Map<Integer, Node> newNodes = new TreeMap<Integer, Node>();
        for (int i = 0; i < numberOfItems; i++) {
            final Track t = SolutionIO.getArcType(trackTypes.get(i));
            final BigDecimal length = SolutionIO.tokenToBigDecimal(trackLengths.get(i));
            // now convert node numbers to Node instances
            final int startNodeId = SolutionIO.tokenToInteger(arcs.get(i).get(0));
            final int endNodeId = SolutionIO.tokenToInteger(arcs.get(i).get(1));
            if (!newNodes.containsKey(startNodeId)) {
                newNodes.put(startNodeId, Node.getNode(startNodeId));
            }
            if (!newNodes.containsKey(endNodeId)) {
                newNodes.put(endNodeId, Node.getNode(endNodeId));
            }
            // and finally create the arc
            final Arc arc = new Arc(t, length, newNodes.get(startNodeId), newNodes.get(endNodeId));
            newArcs.add(arc);
        }
        // store the nodes for future reference
        this.nodes = Collections.unmodifiableMap(newNodes);
        return newArcs;
    }

    private Collection<MaintenanceWindow> initMOW(final DataSetParser p) {
        final List<MaintenanceWindow> mows = new ArrayList<MaintenanceWindow>();
        for (final List<Token> mow : p.getMows()) {
            final MaintenanceWindow newMow = new MaintenanceWindow(this.nodes.get(SolutionIO
                    .tokenToInteger(mow.get(0))), this.nodes.get(SolutionIO.tokenToInteger(mow
                    .get(1))), SolutionIO.tokenToInteger(mow.get(2)), SolutionIO.tokenToInteger(mow
                    .get(3)));
            mows.add(newMow);
        }
        return mows;
    }

    private Train initTrain(final String solutionName, final ParsedTrain t) {
        final boolean hazmat = SolutionIO.tokenToBoolean(t.getHazmat());
        final boolean isWestbound = SolutionIO.tokenToString(t.getDirection()).equals("WESTBOUND");
        final int originalScheduleAdherence = SolutionIO.tokenToInteger(t.getSaStatus());
        final int entryTime = SolutionIO.tokenToInteger(t.getTimeEntry());
        final int wantTime = SolutionIO.tokenToInteger(t.getWantTime().get(1));
        final int tob = SolutionIO.tokenToInteger(t.getTOB());
        final String name = SolutionIO.tokenToString(t.getHeader());
        final BigDecimal length = SolutionIO.tokenToBigDecimal(t.getLength());
        final BigDecimal speedMultiplier = SolutionIO.tokenToBigDecimal(t.getSpeedMultiplier());
        final Node origin = this.nodes.get(SolutionIO.tokenToInteger(t.getNodeOrigin()));
        final Node destination = this.nodes.get(SolutionIO.tokenToInteger(t.getNodeDestination()));
        // just checking; make sure that the direction and target depot match
        final String wantDepot = SolutionIO.tokenToString(t.getWantTime().get(0));
        if (wantDepot.equals("WEST") && !isWestbound || wantDepot.equals("EAST") && isWestbound) {
            SolutionIO.logger.info("Train " + name
                    + " is headed away from the target destination. This bug in " + solutionName
                    + " will be corrected by directing the train to the proper destination.");
        }
        // and now assemble schedules
        final List<ScheduleAdherenceRequirement> sars = new ArrayList<ScheduleAdherenceRequirement>();
        for (int i = 0; i < t.getSchedule().size(); i++) {
            final List<Token> data = t.getSchedule().get(i);
            final Node n = this.nodes.get(SolutionIO.tokenToInteger(data.get(0)));
            final int time = SolutionIO.tokenToInteger(data.get(1));
            final ScheduleAdherenceRequirement sar = new ScheduleAdherenceRequirement(n, time);
            sars.add(sar);
        }
        return new Train(name, length, speedMultiplier, tob, origin, destination, entryTime,
                wantTime, originalScheduleAdherence, sars, hazmat, isWestbound);
    }

    private Collection<Train> initTrains(final String solutionName, final DataSetParser p) {
        // first make sure there's as much trains as stated
        final List<ParsedTrain> origTrains = p.getTrains();
        if (!SolutionIO.tokenToInteger(p.getNumTrains()).equals(origTrains.size())) {
            throw new IllegalStateException(
                    "Number of trains specified doesn't match the actual number of trains!");
        }
        // now parse each train individually
        final List<Train> trains = new ArrayList<Train>();
        for (final ParsedTrain t : origTrains) {
            trains.add(this.initTrain(solutionName, t));
        }
        return trains;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map prepareTexData(final RAS2012Solution solution) {
        final RAS2012ScoreCalculator calc = new RAS2012ScoreCalculator();
        calc.resetWorkingSolution(solution);
        final Map map = new HashMap();
        map.put("name", solution.getName());
        final Set trainsMap = new LinkedHashSet();
        for (final Train t : solution.getTrains()) {
            trainsMap.add(this.prepareTexTrain(solution.getAssignment(t).getItinerary(), solution,
                    calc));
        }
        map.put("trains", trainsMap);
        map.put("cost", -calc.calculateScore().getSoftScore());
        return map;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map prepareTexTrain(final Itinerary itinerary, final RAS2012Solution solution,
            final RAS2012ScoreCalculator calculator) {
        final Train train = itinerary.getTrain();
        final Map map = new HashMap();
        map.put("name", train.getName());
        map.put("delay", SolutionIO.convertMillisToSeconds(itinerary.getDelay()));
        map.put("unpreferredPenalty", calculator.getUnpreferredTracksPenalty(itinerary));
        map.put("stops", this.prepareTexTrainStops(itinerary, solution, calculator));
        map.put("numStops", ((Collection) map.get("stops")).size());
        final long horizon = solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        final long arrival = itinerary.getArrivalTime();
        final boolean isInHorizon = arrival <= horizon;
        final long wantTime = train.getWantTime(TimeUnit.MILLISECONDS);
        map.put("twt", SolutionIO.convertMillisToSeconds(wantTime));
        map.put("twtDiff", isInHorizon ? SolutionIO.convertMillisToSeconds(wantTime - arrival)
                : null);
        map.put("twtPenalty", isInHorizon ? calculator.getWantTimePenalty(itinerary) : "");
        return map;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map prepareTexTrainStop(final Itinerary itinerary, final Node n,
            final RAS2012Solution solution, final RAS2012ScoreCalculator calculator) {
        final Train t = itinerary.getTrain();
        final long horizon = solution.getPlanningHorizon(TimeUnit.MILLISECONDS);
        final long arrival = itinerary.getArrivalTime(n);
        final boolean isInHorizon = arrival <= horizon;
        final Map stop = new HashMap();
        stop.put("node", n.getId());
        stop.put("arrive", SolutionIO.convertMillisToSeconds(arrival));
        if (t.getScheduleAdherenceRequirements().containsKey(n)) {
            final long wantTime = t.getScheduleAdherenceRequirements().get(n)
                    .getTimeSinceStartOfWorld(TimeUnit.MILLISECONDS);
            stop.put("sa", SolutionIO.convertMillisToSeconds(wantTime));
            stop.put("saDiff", isInHorizon ? SolutionIO.convertMillisToSeconds(wantTime - arrival)
                    : null);
            stop.put("saPenalty",
                    isInHorizon ? calculator.getScheduleAdherencePenalty(itinerary, n) : "");
        }
        return stop;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection prepareTexTrainStops(final Itinerary itinerary,
            final RAS2012Solution solution, final RAS2012ScoreCalculator calculator) {
        final Train train = itinerary.getTrain();
        // prepare the set of stops, make sure they are in a proper order
        final List<Node> nodes = new ArrayList<Node>();
        Map<Node, ScheduleAdherenceRequirement> sa = train.getScheduleAdherenceRequirements();
        for (Node n : itinerary.getRoute().getProgression().getNodes()) {
            if (sa.containsKey(n)) {
                nodes.add(n);
            }
        }
        // and now populate the stop data
        final List stops = new ArrayList();
        for (final Node node : nodes) {
            final Map stop = this.prepareTexTrainStop(itinerary, node, solution, calculator);
            stops.add(stop);
        }
        return stops;
    }

    public RAS2012Solution read(final File inputSolutionFile) {
        InputStream is = null;
        try {
            is = new FileInputStream(inputSolutionFile);
            final DataSetParser p = new DataSetParser(is);
            p.parse();
            return this.createSolution(p);
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException("Solution file doesn't exist: " + inputSolutionFile,
                    e);
        } catch (final ParseException e) {
            throw new IllegalArgumentException("Problem parsing solution file: "
                    + inputSolutionFile, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // nothing to do here
                }
            }

        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void writeTex(final RAS2012Solution solution, final File outputSolutionFile) {
        try {
            final Map map = this.prepareTexData(solution);
            map.put("id", outputSolutionFile.getName());
            this.freemarker.getTemplate("schedule.tex.ftl").process(map,
                    new FileWriter(outputSolutionFile));
        } catch (final TemplateException e) {
            SolutionIO.logger.error("Failed processing LaTeX schedule template.", e);
        } catch (final IOException e) {
            SolutionIO.logger.error("Failed writing " + solution.getName() + " into "
                    + outputSolutionFile, e);
        }
    }

    public void writeXML(final RAS2012Solution solution, final File outputSolutionFile) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(outputSolutionFile));
            w.write("###########################################################################");
            w.newLine();
            w.write("<solution territory='" + solution.getName() + "'>");
            w.newLine();
            w.write("\t<trains>");
            w.newLine();
            for (final Train t : solution.getTrains()) {
                SolutionIO.writeTrain(t, solution, w);
            }
            w.write("\t</trains>");
            w.newLine();
            w.write("</solution>");
            w.newLine();
            w.write("###########################################################################");
        } catch (final IOException e) {
            SolutionIO.logger.error("Failed writing solution into file: " + outputSolutionFile, e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (final IOException e) {
                    // nothing to do here
                }
            }
        }
    }
}

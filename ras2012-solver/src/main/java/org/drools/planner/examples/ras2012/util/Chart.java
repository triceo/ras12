package org.drools.planner.examples.ras2012.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chart {

    private static final Logger                       logger  = LoggerFactory
                                                                      .getLogger(Chart.class);

    private final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

    public void addData(final List<Integer> data, final String datasetName) {
        this.dataset.add(data, "", datasetName);
    }

    public BoxAndWhiskerCategoryDataset getDataset() {
        return this.dataset;
    }

    public boolean plot(final File folder, final String filename) {
        final CategoryAxis xAxis = new CategoryAxis("Data Set");
        final NumberAxis yAxis = new NumberAxis("Cost");
        yAxis.setAutoRangeIncludesZero(false);
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        final CategoryPlot plot = new CategoryPlot(this.dataset, xAxis, yAxis, renderer);

        final JFreeChart chart = new JFreeChart(plot);
        chart.setBackgroundPaint(Color.WHITE);
        chart.removeLegend();
        try {
            ChartUtilities.saveChartAsPNG(new File(folder, filename + ".png"), chart, 1024, 768);
            return true;
        } catch (final IOException e) {
            Chart.logger.warn("Charting failed.", e);
            return false;
        }
    }
}
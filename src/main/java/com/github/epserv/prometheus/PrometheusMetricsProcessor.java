package com.github.epserv.prometheus;

import java.io.InputStream;

import com.github.epserv.prometheus.types.*;
import org.jboss.logging.Logger;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A processor is responsible for iterating over a collection of metric families found in a specific
 * data format and invoking a walker during the iteration so the metric families can be processed.
 */
public abstract class PrometheusMetricsProcessor<T> {
    private static final Logger log = Logger.getLogger(PrometheusMetricsProcessor.class);

    private final @NotNull InputStream inputStream;
    private final @NotNull PrometheusMetricsWalker walker;

    /**
     * @param inputStream where the Prometheus metrics are that the walker will traverse.
     * @param theWalker the actual object that will be notified about the metrics as they are encountered
     */
    public PrometheusMetricsProcessor(@NotNull InputStream inputStream, @NotNull PrometheusMetricsWalker theWalker) {
        this.inputStream = inputStream;
        this.walker = theWalker;
    }

    /**
     * This will iterate over a set of metrics that are produced by the
     * {@link #createPrometheusMetricDataParser() parser} and will notify the {@link #getWalker() walker}
     * of each metric found.
     */
    public void walk() {
        // tell the walker we are starting
        walker.walkStart();

        int totalMetrics = 0;
        int familyIndex = 0;

        try {
            PrometheusMetricDataParser<T> parser = createPrometheusMetricDataParser();
            T metricFamily = parser.parse(); // prime the pump

            while (metricFamily != null) {
                MetricFamily convertedMetricFamily = convert(metricFamily);

                // let the walker know we are traversing a new family of metrics
                walker.walkMetricFamily(convertedMetricFamily, familyIndex++);

                // walk through each metric in the family
                int metricIndex = 0;

                for (Metric metric : convertedMetricFamily.getMetrics()) {
                    switch (convertedMetricFamily.getType()) {
                        case COUNTER -> walker.walkCounterMetric(convertedMetricFamily, (Counter) metric, metricIndex);
                        case GAUGE -> walker.walkGaugeMetric(convertedMetricFamily, (Gauge) metric, metricIndex);
                        case SUMMARY -> walker.walkSummaryMetric(convertedMetricFamily, (Summary) metric, metricIndex);
                        case HISTOGRAM -> walker.walkHistogramMetric(convertedMetricFamily, (Histogram) metric, metricIndex);
                    }

                    metricIndex++;
                }

                // finished processing the metrics for the current family
                totalMetrics += convertedMetricFamily.getMetrics().size();

                // go to the next metric family
                metricFamily = parser.parse();
            }
        } catch (Exception e) {
            log.debugf(e, "Error while processing binary data");
        }

        // tell the walker we have finished
        walker.walkFinish(familyIndex, totalMetrics);
    }

    /**
     * @return the input stream where the metric family data in a specific data format is found
     */
    @Contract(pure = true)
    protected @NotNull InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the object that will iterate over the found metric data
     */
    @Contract(pure = true)
    protected @NotNull PrometheusMetricsWalker getWalker() {
        return walker;
    }

    /**
     * @return a new parser instance that can be used to parse the formatted data
     *         found in the {@link #getInputStream() input stream}.
     */
    protected abstract @NotNull PrometheusMetricDataParser<T> createPrometheusMetricDataParser();

    /**
     * This method converts the metrics from the specific data format found in the input stream
     * to the common metric format.
     *
     * @param metricFamily the metric family (and its metrics) that need to be converted
     * @return the common MetricFamily object
     */
    protected abstract @NotNull MetricFamily convert(@NotNull T metricFamily);
}

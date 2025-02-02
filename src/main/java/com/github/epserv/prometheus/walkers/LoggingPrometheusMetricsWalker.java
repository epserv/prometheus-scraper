package com.github.epserv.prometheus.walkers;

import java.util.Map;

import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.Summary;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import com.github.epserv.prometheus.types.Gauge;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This implementation simply logs the metric values.
 */
public class LoggingPrometheusMetricsWalker implements PrometheusMetricsWalker {
    private static final Logger log = Logger.getLogger(LoggingPrometheusMetricsWalker.class);
    private final @NotNull Level logLevel;

    public LoggingPrometheusMetricsWalker() {
        this(null);
    }

    public LoggingPrometheusMetricsWalker(@Nullable Level logLevel) {
        this.logLevel = logLevel != null ? logLevel : Level.DEBUG;
    }

    @Override
    public void walkStart() {
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
    }

    @Override
    public void walkMetricFamily(@NotNull MetricFamily family, int index) {
        log.logf(getLogLevel(), "Metric Family [%s] of type [%s] has [%d] metrics: %s",
                family.getName(),
                family.getType(),
                family.getMetrics().size(),
                family.getHelp());
    }

    @Override
    public void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter metric, int index) {
        log.logf(getLogLevel(), "COUNTER: %s%s=%f",
                metric.getName(),
                buildLabelListString(metric.getLabels()),
                metric.getValue());
    }

    @Override
    public void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge metric, int index) {
        log.logf(getLogLevel(), "GAUGE: %s%s=%f",
                metric.getName(),
                buildLabelListString(metric.getLabels()),
                metric.getValue());
    }

    @Override
    public void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary metric, int index) {
        log.logf(getLogLevel(), "SUMMARY: %s%s: count=%d, sum=%f, quantiles=%s",
                metric.getName(),
                buildLabelListString(metric.getLabels()),
                metric.getSampleCount(),
                metric.getSampleSum(),
                metric.getQuantiles());
    }

    @Override
    public void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram metric, int index) {
        log.logf(getLogLevel(), "HISTOGRAM: %s%s: count=%d, sum=%f, buckets=%s",
                metric.getName(),
                buildLabelListString(metric.getLabels()),
                metric.getSampleCount(),
                metric.getSampleSum(),
                metric.getBuckets());
    }

    /**
     * The default implementations of the walk methods will log the metric data with this given log level.
     *
     * @return the log level
     */
    @Contract(pure = true)
    protected @NotNull Level getLogLevel() {
        return this.logLevel;
    }

    protected @NotNull String buildLabelListString(@Nullable Map<String, String> labels) {
        return buildLabelListString(labels, "{", "}");
    }
}

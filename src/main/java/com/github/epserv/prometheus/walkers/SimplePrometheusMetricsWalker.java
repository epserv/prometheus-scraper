package com.github.epserv.prometheus.walkers;

import java.net.URL;

import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.Summary;
import com.github.epserv.prometheus.types.Gauge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimplePrometheusMetricsWalker implements PrometheusMetricsWalker {

    private final @Nullable URL url;

    public SimplePrometheusMetricsWalker() {
        this(null);
    }

    /**
     * Use this constructor if you know the URL where the metric data came from.
     *
     * @param url the protocol endpoint that supplied the Prometheus metric data
     */
    public SimplePrometheusMetricsWalker(@Nullable URL url) {
        this.url = url;
    }

    @Override
    public void walkStart() {
        if (url != null) {
            System.out.println("Scraping metrics from Prometheus protocol endpoint: " + url);
        }
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
        if (metricsProcessed == 0) {
            System.out.println("There are no metrics");
        }
    }

    @Override
    public void walkMetricFamily(@NotNull MetricFamily family, int index) {
        System.out.printf("* %s (%s): %s\n", family.getName(), family.getType(), family.getHelp());
    }

    @Override
    public void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter metric, int index) {
        System.out.printf("  +%2d. %s%s [%f]\n",
                index,
                metric.getName(),
                buildLabelListString(metric.getLabels(), "{", "}"),
                metric.getValue());
    }

    @Override
    public void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge metric, int index) {
        System.out.printf("  +%2d. %s%s [%f]\n",
                index,
                metric.getName(),
                buildLabelListString(metric.getLabels(), "{", "}"),
                metric.getValue());
    }

    @Override
    public void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary metric, int index) {
        System.out.printf("  +%2d. %s%s [%d/%f] {%s}\n",
                index,
                metric.getName(),
                buildLabelListString(metric.getLabels(), "{", "}"),
                metric.getSampleCount(),
                metric.getSampleSum(),
                metric.getQuantiles());
    }

    @Override
    public void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram metric, int index) {
        System.out.printf("  +%2d. %s%s [%d/%f] {%s}\n",
                index,
                metric.getName(),
                buildLabelListString(metric.getLabels(), "{", "}"),
                metric.getSampleCount(),
                metric.getSampleSum(),
                metric.getBuckets());
    }
}

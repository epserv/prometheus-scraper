package com.github.epserv.prometheus.walkers;

import java.net.URL;

import com.github.epserv.prometheus.Util;
import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Gauge;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.MetricType;
import com.github.epserv.prometheus.types.Summary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XMLPrometheusMetricsWalker implements PrometheusMetricsWalker {

    private final @Nullable URL url;

    public XMLPrometheusMetricsWalker() {
        this(null);
    }

    /**
     * Use this constructor if you know the URL where the metric data came from.
     *
     * @param url the protocol endpoint that supplied the Prometheus metric data
     */
    public XMLPrometheusMetricsWalker(@Nullable URL url) {
        this.url = url;
    }

    @Override
    public void walkStart() {
        System.out.print("<metricFamilies>\n");

        // only provide the URL endpoint element if we know the URL where the metrics came from
        if (url != null) {
            System.out.printf("  <url>%s</url>\n", url);
        }
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
        if (familiesProcessed > 0) {
            System.out.print("  </metricFamily>\n");
        }
        System.out.println("</metricFamilies>");
    }

    @Override
    public void walkMetricFamily(@NotNull MetricFamily family, int index) {
        if (index > 0) {
            System.out.print("  </metricFamily>\n");
        }

        System.out.print("  <metricFamily>\n");
        System.out.printf("    <name>%s</name>\n", family.getName());
        System.out.printf("    <type>%s</type>\n", family.getType());
        System.out.printf("    <help>%s</help>\n", family.getHelp());
    }

    @Override
    public void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter metric, int index) {
        System.out.print("    <metric>\n");
        System.out.printf("      <name>%s</name>\n", family.getName());
        System.out.printf("      <type>%s</type>\n", MetricType.COUNTER);
        System.out.printf("      <labels>%s</labels>\n", buildLabelListString(metric.getLabels(), null, null));
        System.out.printf("      <value>%s</value>\n", Util.convertDoubleToString(metric.getValue()));
        System.out.print("    </metric>\n");
    }

    @Override
    public void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge metric, int index) {
        System.out.print("    <metric>\n");
        System.out.printf("      <name>%s</name>\n", family.getName());
        System.out.printf("      <type>%s</type>\n", MetricType.GAUGE);
        System.out.printf("      <labels>%s</labels>\n", buildLabelListString(metric.getLabels(), null, null));
        System.out.printf("      <value>%s</value>\n", Util.convertDoubleToString(metric.getValue()));
        System.out.print("    </metric>\n");
    }

    @Override
    public void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary metric, int index) {
        System.out.print("    <metric>\n");
        System.out.printf("      <name>%s</name>\n", family.getName());
        System.out.printf("      <type>%s</type>\n", MetricType.SUMMARY);
        System.out.printf("      <labels>%s</labels>\n", buildLabelListString(metric.getLabels(), null, null));
        System.out.printf("      <count>%d</count>\n", metric.getSampleCount());
        System.out.printf("      <sum>%s</sum>\n", Util.convertDoubleToString(metric.getSampleSum()));
        if (!metric.getQuantiles().isEmpty()) {
            System.out.print("      <quantiles>\n");
            for (Summary.Quantile quantile : metric.getQuantiles()) {
                System.out.printf("        <quantile>%s</quantile>\n", quantile);
            }
            System.out.print("      </quantiles>\n");
        }
        System.out.print("    </metric>\n");
    }

    @Override
    public void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram metric, int index) {
        System.out.print("    <metric>\n");
        System.out.printf("      <name>%s</name>\n", family.getName());
        System.out.printf("      <type>%s</type>\n", MetricType.HISTOGRAM);
        System.out.printf("      <labels>%s</labels>\n", buildLabelListString(metric.getLabels(), null, null));
        System.out.printf("      <count>%d</count>\n", metric.getSampleCount());
        System.out.printf("      <sum>%s</sum>\n", Util.convertDoubleToString(metric.getSampleSum()));
        if (!metric.getBuckets().isEmpty()) {
            System.out.print("      <buckets>\n");
            for (Histogram.Bucket bucket : metric.getBuckets()) {
                System.out.printf("        <bucket>%s</bucket>\n", bucket);
            }
            System.out.print("      </bucket>\n");
        }
        System.out.print("    </metric>\n");
    }
}

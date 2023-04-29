package com.github.epserv.prometheus.walkers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.github.epserv.prometheus.Util;
import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.Summary;
import com.github.epserv.prometheus.types.Gauge;
import org.jetbrains.annotations.NotNull;

public class JSONPrometheusMetricsWalker implements PrometheusMetricsWalker {

    @Override
    public void walkStart() {
        System.out.println("[");
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
        if (familiesProcessed > 0) {
            System.out.println("    ]");
            System.out.println("  }");
        }
        System.out.println("]");
    }

    @Override
    public void walkMetricFamily(@NotNull MetricFamily familyInfo, int index) {
        if (index > 0) {
            System.out.print("    ]\n");
            System.out.print("  },\n");
        }

        System.out.print("  {\n");
        System.out.printf("    \"name\":\"%s\",\n", familyInfo.getName());
        System.out.printf("    \"help\":\"%s\",\n", familyInfo.getHelp());
        System.out.printf("    \"type\":\"%s\",\n", familyInfo.getType());
        System.out.print("    \"metrics\":[\n");
    }

    @Override
    public void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter metric, int index) {
        System.out.print("      {\n");
        outputLabels(metric.getLabels());
        System.out.printf("        \"value\":\"%s\"\n", Util.convertDoubleToString(metric.getValue()));
        if ((index + 1) == family.getMetrics().size()) {
            System.out.print("      }\n");
        } else {
            System.out.print("      },\n"); // there are more coming
        }
    }

    @Override
    public void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge metric, int index) {
        System.out.print("      {\n");
        outputLabels(metric.getLabels());
        System.out.printf("        \"value\":\"%s\"\n", Util.convertDoubleToString(metric.getValue()));
        if ((index + 1) == family.getMetrics().size()) {
            System.out.print("      }\n");
        } else {
            System.out.print("      },\n"); // there are more coming
        }
    }

    @Override
    public void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary metric, int index) {
        System.out.print("      {\n");
        outputLabels(metric.getLabels());
        if (!metric.getQuantiles().isEmpty()) {
            System.out.print("        \"quantiles\":{\n");
            Iterator<Summary.Quantile> iter = metric.getQuantiles().iterator();
            while (iter.hasNext()) {
                Summary.Quantile quantile = iter.next();
                System.out.printf("          \"%f\":\"%f\"%s\n",
                        quantile.quantile(), quantile.value(), (iter.hasNext()) ? "," : "");
            }
            System.out.print("        },\n");
        }
        System.out.printf("        \"count\":\"%d\",\n", metric.getSampleCount());
        System.out.printf("        \"sum\":\"%s\"\n", Util.convertDoubleToString(metric.getSampleSum()));
        if ((index + 1) == family.getMetrics().size()) {
            System.out.print("      }\n");
        } else {
            System.out.print("      },\n"); // there are more coming
        }
    }

    @Override
    public void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram metric, int index) {
        System.out.print("      {\n");
        outputLabels(metric.getLabels());
        if (!metric.getBuckets().isEmpty()) {
            System.out.print("        \"buckets\":{\n");
            Iterator<Histogram.Bucket> iter = metric.getBuckets().iterator();
            while (iter.hasNext()) {
                Histogram.Bucket bucket = iter.next();
                System.out.printf("          \"%f\":\"%d\"%s\n",
                        bucket.upperBound(), bucket.cumulativeCount(), (iter.hasNext()) ? "," : "");
            }
            System.out.print("        },\n");
        }
        System.out.printf("        \"count\":\"%d\",\n", metric.getSampleCount());
        System.out.printf("        \"sum\":\"%s\"\n", Util.convertDoubleToString(metric.getSampleSum()));
        if ((index + 1) == family.getMetrics().size()) {
            System.out.print("      }\n");
        } else {
            System.out.print("      },\n"); // there are more coming
        }
    }

    private void outputLabels(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        System.out.print("        \"labels\":{\n");
        Iterator<Entry<String, String>> iter = labels.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> labelPair = iter.next();
            String comma = (iter.hasNext()) ? "," : "";
            System.out.printf("          \"%s\":\"%s\"%s\n", labelPair.getKey(), labelPair.getValue(), comma);
        }
        System.out.print("        },\n");
    }
}

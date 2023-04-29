package com.github.epserv.prometheus.walkers;

import java.util.Map;

import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.Summary;
import com.github.epserv.prometheus.types.Gauge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementors iterate a collection of metric families and their metrics.
 */
public interface PrometheusMetricsWalker {

    /**
     * Called when a walk has been started.
     */
    void walkStart();

    /**
     * Called when a walk has traversed all the metrics.
     * @param familiesProcessed total number of families processed
     * @param metricsProcessed total number of metrics across all families processed
     */
    void walkFinish(int familiesProcessed, int metricsProcessed);

    /**
     * Called when a new metric family is about to be traversed.
     *
     * @param family information about the family being traversed such as the name, help description, etc.
     * @param index index of the family being processed, where 0 is the first one.
     */
    void walkMetricFamily(@NotNull MetricFamily family, int index);

    /**
     * Called when a new counter metric is found.
     *
     * @param family information about the family being traversed such as the name, help description, etc.
     * @param counter the metric being processed
     * @param index index of the metric being processed, where 0 is the first one.
     */
    void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter counter, int index);

    /**
     * Called when a new gauge metric is found.
     *
     * @param family information about the family being traversed such as the name, help description, etc.
     * @param gauge the metric being processed
     * @param index index of the metric being processed, where 0 is the first one.
     */
    void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge gauge, int index);

    /**
     * Called when a new summary metric is found.
     *
     * @param family information about the family being traversed such as the name, help description, etc.
     * @param summary the metric being processed
     * @param index index of the metric being processed, where 0 is the first one.
     */
    void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary summary, int index);

    /**
     * Called when a new histogram metric is found.
     *
     * @param family information about the family being traversed such as the name, help description, etc.
     * @param histogram the metric being processed
     * @param index index of the metric being processed, where 0 is the first one.
     */
    void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram histogram, int index);

    /**
     * Convenience method that takes the given label list and returns a string in the form of
     * "labelName1=labelValue1,labelName2=labelValue2,..."
     *
     * @param labels the label list
     * @param prefix if not null, these characters will prefix the label list
     * @param suffix if not null, these characters will suffix the label list
     * @return the string form of the labels, optionally prefixed and suffixed
     */
    default @NotNull String buildLabelListString(@Nullable Map<String, String> labels, @Nullable String prefix, @Nullable String suffix) {
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";
        if (labels == null) return String.format("%s%s", prefix, suffix);

        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, String> pair : labels.entrySet()) {
            if (str.length() > 0) {
                str.append(",");
            }
            str.append(pair.getKey()).append("=").append(pair.getValue());
        }
        return String.format("%s%s%s", prefix, str, suffix);
    }

}

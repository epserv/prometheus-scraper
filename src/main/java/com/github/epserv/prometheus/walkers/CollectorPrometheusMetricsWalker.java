package com.github.epserv.prometheus.walkers;

import java.util.ArrayList;
import java.util.List;

import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Gauge;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.Summary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This simply collects all metrics in all families and provides a list to the families.
 */
public class CollectorPrometheusMetricsWalker implements PrometheusMetricsWalker {

    private List<@NotNull MetricFamily> finishedList;
    private boolean finished;

    /**
     * @return indicates if this walker has finished processing all metric families.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * @return if this walker has finished processing all metric families, this will return the list of the
     *         metric families processed. If the walker hasn't finished yet, null is returned.
     */
    public @Nullable List<@NotNull MetricFamily> getAllMetricFamilies() {
        return finished ? finishedList : null;
    }

    @Override
    public void walkStart() {
        finished = false;
        finishedList = new ArrayList<>();
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
        finished = true;
    }

    @Override
    public void walkMetricFamily(@NotNull MetricFamily family, int index) {
        finishedList.add(family);
    }

    @Override
    public void walkCounterMetric(@NotNull MetricFamily family, @NotNull Counter metric, int index) {
    }

    @Override
    public void walkGaugeMetric(@NotNull MetricFamily family, @NotNull Gauge metric, int index) {
    }

    @Override
    public void walkSummaryMetric(@NotNull MetricFamily family, @NotNull Summary metric, int index) {
    }

    @Override
    public void walkHistogramMetric(@NotNull MetricFamily family, @NotNull Histogram metric, int index) {
    }
}

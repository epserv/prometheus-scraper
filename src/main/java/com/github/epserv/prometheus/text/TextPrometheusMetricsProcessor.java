package com.github.epserv.prometheus.text;

import java.io.InputStream;

import com.github.epserv.prometheus.PrometheusMetricsProcessor;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This will iterate over a list of Prometheus metrics that are given as text data.
 */
public class TextPrometheusMetricsProcessor extends PrometheusMetricsProcessor<MetricFamily> {
    public TextPrometheusMetricsProcessor(@NotNull InputStream inputStream, @NotNull PrometheusMetricsWalker theWalker) {
        super(inputStream, theWalker);
    }

    @Override
    @Contract("-> new")
    public @NotNull TextPrometheusMetricDataParser createPrometheusMetricDataParser() {
        return new TextPrometheusMetricDataParser(getInputStream());
    }

    @Override
    @Contract(value = "_ -> param1", pure = true)
    protected @NotNull MetricFamily convert(@NotNull MetricFamily metricFamily) {
        return metricFamily; // no conversion necessary - our text parser already uses the common api
    }

}

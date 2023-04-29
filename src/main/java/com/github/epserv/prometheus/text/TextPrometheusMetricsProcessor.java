package com.github.epserv.prometheus.text;

import java.io.InputStream;

import com.github.epserv.prometheus.PrometheusMetricsProcessor;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;

/**
 * This will iterate over a list of Prometheus metrics that are given as text data.
 */
public class TextPrometheusMetricsProcessor extends PrometheusMetricsProcessor<MetricFamily> {
    public TextPrometheusMetricsProcessor(InputStream inputStream, PrometheusMetricsWalker theWalker) {
        super(inputStream, theWalker);
    }

    @Override
    public TextPrometheusMetricDataParser createPrometheusMetricDataParser() {
        return new TextPrometheusMetricDataParser(getInputStream());
    }

    @Override
    protected MetricFamily convert(MetricFamily metricFamily) {
        return metricFamily; // no conversion necessary - our text parser already uses the common api
    }

}

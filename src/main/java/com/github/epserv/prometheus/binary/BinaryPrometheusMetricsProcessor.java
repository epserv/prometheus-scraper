package com.github.epserv.prometheus.binary;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.epserv.prometheus.PrometheusMetricsProcessor;
import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Gauge;
import com.github.epserv.prometheus.types.MetricType;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;
import io.prometheus.client.Metrics.LabelPair;
import io.prometheus.client.Metrics.Metric;
import io.prometheus.client.Metrics.MetricFamily;
import io.prometheus.client.Metrics.Quantile;
import io.prometheus.client.Metrics.Summary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This will iterate over a list of Prometheus metrics that are given as binary protocol buffer data.
 */
public class BinaryPrometheusMetricsProcessor extends PrometheusMetricsProcessor<MetricFamily> {
    public BinaryPrometheusMetricsProcessor(@NotNull InputStream inputStream, @NotNull PrometheusMetricsWalker theWalker) {
        super(inputStream, theWalker);
    }

    @Override
    @Contract("-> new")
    public @NotNull BinaryPrometheusMetricDataParser createPrometheusMetricDataParser() {
        return new BinaryPrometheusMetricDataParser(getInputStream());
    }

    @Override
    @Contract("_ -> new")
    protected com.github.epserv.prometheus.types.@NotNull MetricFamily convert(@NotNull MetricFamily family) {
        com.github.epserv.prometheus.types.MetricFamily.Builder convertedFamilyBuilder;
        MetricType convertedFamilyType = MetricType.valueOf(family.getType().name());

        convertedFamilyBuilder = new com.github.epserv.prometheus.types.MetricFamily.Builder();
        convertedFamilyBuilder.setName(family.getName());
        convertedFamilyBuilder.setHelp(family.getHelp());
        convertedFamilyBuilder.setType(convertedFamilyType);

        for (Metric metric : family.getMetricList()) {
            com.github.epserv.prometheus.types.Metric.Builder<?, ?> convertedMetricBuilder = null;
            switch (convertedFamilyType) {
                case COUNTER:
                    convertedMetricBuilder = new Counter.Builder().setValue(metric.getCounter().getValue());
                    break;
                case GAUGE:
                    convertedMetricBuilder = new Gauge.Builder().setValue(metric.getGauge().getValue());
                    break;
                case SUMMARY:
                    Summary summary = metric.getSummary();
                    List<Quantile> pqList = summary.getQuantileList();
                    List<com.github.epserv.prometheus.types.Summary.Quantile> hqList;
                    hqList = new ArrayList<>(pqList.size());
                    for (Quantile pq : pqList) {
                        com.github.epserv.prometheus.types.Summary.Quantile hq;
                        hq = new com.github.epserv.prometheus.types.Summary.Quantile(pq.getQuantile(), pq.getValue());
                        hqList.add(hq);
                    }
                    convertedMetricBuilder = new com.github.epserv.prometheus.types.Summary.Builder()
                            .setSampleCount(metric.getSummary().getSampleCount())
                            .setSampleSum(metric.getSummary().getSampleSum())
                            .addQuantiles(hqList);
                    break;
                case HISTOGRAM:
                    /* NO HISTOGRAM SUPPORT IN PROMETHEUS JAVA MODEL API 0.0.2. Uncomment when 0.0.3 is released
                    Histogram histogram = metric.getHistogram();
                    List<Bucket> pbList = histogram.getBucketList();
                    List<com.github.epserv.prometheus.types.Histogram.Bucket> hbList;
                    hbList = new ArrayList<>(pbList.size());
                    for (Bucket pb : pbList) {
                        com.github.epserv.prometheus.types.Histogram.Bucket hb;
                        hb = new com.github.epserv.prometheus.types.Histogram.Bucket(pb.getUpperBound(),
                                pb.getCumulativeCount());
                        hbList.add(hb);
                    }
                    convertedMetricBuilder = new com.github.epserv.prometheus.types.Histogram.Builder()
                            .setSampleCount(metric.getHistogram().getSampleCount())
                            .setSampleSum(metric.getHistogram().getSampleSum())
                            .addBuckets(hbList);
                    */
                    break;
            }
            if (convertedMetricBuilder != null) {
                convertedMetricBuilder.setName(family.getName());
                for (LabelPair labelPair : metric.getLabelList()) {
                    convertedMetricBuilder.addLabel(labelPair.getName(), labelPair.getValue());
                }
                convertedFamilyBuilder.addMetric(convertedMetricBuilder.build());
            }
        }

        return convertedFamilyBuilder.build();
    }
}

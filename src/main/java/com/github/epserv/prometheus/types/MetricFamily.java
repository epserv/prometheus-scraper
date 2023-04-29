package com.github.epserv.prometheus.types;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains all metrics within a family (that is, of the same name). All metrics in a family have the same type.
 */
public class MetricFamily {

    public static class Builder {
        private String name;
        private String help;
        private MetricType type;
        private List<@NotNull Metric> metrics;

        public Builder setName(@NotNull String name) {
            this.name = name;
            return this;
        }

        public Builder setHelp(@Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder setType(@NotNull MetricType type) {
            this.type = type;
            return this;
        }

        public Builder addMetric(@NotNull Metric metric) {
            if (metrics == null) {
                metrics = new ArrayList<>();
            }
            metrics.add(metric);
            return this;
        }

        @Contract("-> new")
        public @NotNull MetricFamily build() {
            return new MetricFamily(this);
        }
    }

    private final @NotNull String name;
    private final @Nullable String help;
    private final @NotNull MetricType type;
    private final @Nullable List<@NotNull Metric> metrics;

    protected MetricFamily(@NotNull Builder builder) {
        if (builder.name == null) throw new IllegalArgumentException("Need to set name");
        if (builder.type == null) throw new IllegalArgumentException("Need to set type");

        Class<? extends Metric> expectedMetricClassType = switch (builder.type) {
            case COUNTER -> Counter.class;
            case GAUGE -> Gauge.class;
            case SUMMARY -> Summary.class;
            case HISTOGRAM -> Histogram.class;
        };

        // make sure all the metrics in the family are of the expected type
        if (builder.metrics != null && !builder.metrics.isEmpty()) {
            for (Metric metric : builder.metrics) {
                if (!expectedMetricClassType.isInstance(metric)) {
                    throw new IllegalArgumentException(
                            String.format("Metric type is [%s] so instances of class [%s] are expected, "
                                    + "but got metric object of type [%s]",
                                    builder.type, expectedMetricClassType.getName(), metric.getClass().getName()));
                }
            }

        }

        this.name = builder.name;
        this.help = builder.help;
        this.type = builder.type;
        this.metrics = builder.metrics;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getHelp() {
        return help;
    }

    public @NotNull MetricType getType() {
        return type;
    }

    public @NotNull List<@NotNull Metric> getMetrics() {
        if (metrics == null) {
            return Collections.emptyList();
        }
        return metrics;
    }
}

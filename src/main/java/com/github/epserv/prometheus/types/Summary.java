package com.github.epserv.prometheus.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.epserv.prometheus.Util;

public class Summary extends Metric {

    public static class Builder extends Metric.Builder<Summary, Builder> {
        private long sampleCount = 0;
        private double sampleSum = Double.NaN;
        private List<Quantile> quantiles;

        public Summary build() {
            return new Summary(this);
        }

        public Builder setSampleCount(long sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        public Builder setSampleSum(double sampleSum) {
            this.sampleSum = sampleSum;
            return this;
        }

        public Builder addQuantile(double quantile, double value) {
            if (quantiles == null) {
                quantiles = new ArrayList<>();
            }
            quantiles.add(new Quantile(quantile, value));
            return this;
        }

        public Builder addQuantiles(List<Quantile> quantiles) {
            if (this.quantiles == null) {
                this.quantiles = new ArrayList<>();
            }
            this.quantiles.addAll(quantiles);
            return this;
        }
    }

    public record Quantile(double quantile, double value) {
        @Override
        public String toString() {
            return String.format("%s:%s", Util.convertDoubleToString(quantile), Util.convertDoubleToString(value));
        }
    }

    private final long sampleCount;
    private final double sampleSum;
    private final List<Quantile> quantiles;

    private Summary(Builder builder) {
        super(builder);
        getLabels().remove("quantile");
        this.sampleCount = builder.sampleCount;
        this.sampleSum = builder.sampleSum;
        this.quantiles = builder.quantiles;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public double getSampleSum() {
        return sampleSum;
    }

    public List<Quantile> getQuantiles() {
        if (quantiles == null) {
            return Collections.emptyList();
        }
        return quantiles;
    }
}

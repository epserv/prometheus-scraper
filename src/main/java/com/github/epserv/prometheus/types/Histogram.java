package com.github.epserv.prometheus.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.epserv.prometheus.Util;

public class Histogram extends Metric {

    public static class Builder extends Metric.Builder<Histogram, Builder> {
        private long sampleCount = 0;
        private double sampleSum = Double.NaN;
        private List<Bucket> buckets;

        public Histogram build() {
            return new Histogram(this);
        }

        public Builder setSampleCount(long sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        public Builder setSampleSum(double sampleSum) {
            this.sampleSum = sampleSum;
            return this;
        }

        public Builder addBucket(double upperBound, long cumulativeCount) {
            if (buckets == null) {
                buckets = new ArrayList<>();
            }
            buckets.add(new Bucket(upperBound, cumulativeCount));
            return this;
        }

        public Builder addBuckets(List<Bucket> buckets) {
            if (this.buckets == null) {
                this.buckets = new ArrayList<>();
            }
            this.buckets.addAll(buckets);
            return this;
        }
    }

    public record Bucket(double upperBound, long cumulativeCount) {
        @Override
        public String toString() {
            return String.format("%s:%d", Util.convertDoubleToString(upperBound), cumulativeCount);
        }
    }

    private final long sampleCount;
    private final double sampleSum;
    private final List<Bucket> buckets;

    private Histogram(Builder builder) {
        super(builder);
        getLabels().remove("le");
        this.sampleCount = builder.sampleCount;
        this.sampleSum = builder.sampleSum;
        this.buckets = builder.buckets;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public double getSampleSum() {
        return sampleSum;
    }

    public List<Bucket> getBuckets() {
        if (buckets == null) {
            return Collections.emptyList();
        }
        return buckets;
    }
}

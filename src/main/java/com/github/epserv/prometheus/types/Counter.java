package com.github.epserv.prometheus.types;

public class Counter extends Metric {

    public static class Builder extends Metric.Builder<Counter, Builder> {
        private double value = Double.NaN;

        public Counter build() {
            return new Counter(this);
        }

        public Builder setValue(double value) {
            this.value = value;
            return this;
        }
    }

    private final double value;

    public Counter(Builder builder) {
        super(builder);
        this.value = builder.value;
    }

    public double getValue() {
        return value;
    }
}

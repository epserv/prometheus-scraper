package com.github.epserv.prometheus.types;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Superclass to all metrics. All metrics have name and labels.
 */
public abstract class Metric {

    public abstract static class Builder<T extends Metric, B extends Builder<T, B>> {
        private String name;
        private Map<String, String> labels;

        public B setName(String name) {
            this.name = name;
            return (B) this;
        }

        public B addLabel(String name, String value) {
            if (labels == null) {
                labels = new LinkedHashMap<>(); // used linked hash map to retain ordering
            }
            labels.put(name, value);
            return (B) this;
        }

        public B addLabels(Map<String, String> map) {
            if (labels == null) {
                labels = new LinkedHashMap<>(); // used linked hash map to retain ordering
            }
            labels.putAll(map);
            return (B) this;
        }

        public abstract T build();
    }

    private final @NotNull String name;
    private final @Nullable Map<String, String> labels;

    @Contract(pure = true)
    protected Metric(@NotNull Builder<?, ?> builder) {
        if (builder.name == null) throw new IllegalArgumentException("Need to set name");

        this.name = builder.name;
        this.labels = builder.labels;
    }

    @Contract(pure = true)
    public @NotNull String getName() {
        return name;
    }

    @Contract(pure = true)
    public @NotNull Map<String, String> getLabels() {
        if (labels == null) {
            return Collections.emptyMap();
        }
        return labels;
    }
}

package com.github.epserv.prometheus;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The supported Prometheus data formats.
 */
public enum PrometheusDataFormat {
    TEXT("text/plain"),
    BINARY("application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited");

    private final @NotNull String contentType;

    PrometheusDataFormat(@NotNull String contentType) {
        this.contentType = contentType;
    }

    @Contract(pure = true)
    public @NotNull String getContentType() {
        return this.contentType;
    }
}

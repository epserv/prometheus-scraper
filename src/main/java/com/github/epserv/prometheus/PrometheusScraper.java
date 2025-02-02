package com.github.epserv.prometheus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;

import com.github.epserv.prometheus.text.TextPrometheusMetricsProcessor;
import com.github.epserv.prometheus.walkers.CollectorPrometheusMetricsWalker;
import org.jboss.logging.Logger;
import com.github.epserv.prometheus.binary.BinaryPrometheusMetricsProcessor;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Given a Prometheus protocol endpoint, this will scrape the Prometheus data it finds there.
 * {@link #scrape()} is typically the API most consumers will want to use. Given a URL or file, this is
 * able to give you all the metric data found there, regardless of the format of the data.
 */
public class PrometheusScraper {
    private static final Logger log = Logger.getLogger(PrometheusScraper.class);

    private final @NotNull URL url;
    private final @Nullable PrometheusDataFormat knownDataFormat;
    private final @Nullable String authorization;

    // see openConnection() for where this is used
    protected static class OpenConnectionDetails {
        private final @NotNull InputStream inputStream;
        private final @Nullable String contentType;

        public OpenConnectionDetails(@NotNull InputStream is, @Nullable String contentType) {
            this.inputStream = is;
            this.contentType = contentType;
        }

        @Contract(pure = true)
        public @NotNull InputStream getInputStream() {
            return this.inputStream;
        }

        @Contract(pure = true)
        public @Nullable String getContentType() {
            return this.contentType;
        }
    }

    public PrometheusScraper(@Nullable String host, int port, @Nullable String context) throws MalformedURLException {
        this(host, port, context, null);
    }

    public PrometheusScraper(@Nullable String host, int port, @Nullable String context, @Nullable String authorization) throws MalformedURLException {
        if (host == null) {
            host = "127.0.0.1";
        }
        if (port == 0) {
            port = 9090;
        }
        if (context == null || context.isEmpty()) {
            context = "/metrics";
        }
        this.url = new URL("http", host, port, context);
        this.knownDataFormat = null;
        this.authorization = authorization;
        log.debugf("Will scrape Prometheus data from URL [%s]", this.url);
    }

    public PrometheusScraper(@NotNull URL url) {
        this(url, null);
    }

    /**
     * This constructor allows you to explicitly indicate what data format is expected.
     * If the URL cannot provide a content type, this data format will determine what data format
     * will be assumed. If the URL does provide a content type, the given data format is ignored.
     * This is useful if you are providing a URL that actually refers to a file in which case
     * the URL connection will not be able to provide a content type.
     *
     * @see #PrometheusScraper(File, PrometheusDataFormat)
     *
     * @param url the URL where the Prometheus metric data is found
     * @param dataFormat the data format of the metric data found at the URL, or null if
     *                   the URL endpoint can provide it for us via content negotiation.
     */
    public PrometheusScraper(@NotNull URL url, @Nullable PrometheusDataFormat dataFormat) {
        this(url, dataFormat, null);
    }

    /**
     * This constructor allows you to explicitly indicate what data format is expected.
     * If the URL cannot provide a content type, this data format will determine what data format
     * will be assumed. If the URL does provide a content type, the given data format is ignored.
     * This is useful if you are providing a URL that actually refers to a file in which case
     * the URL connection will not be able to provide a content type.
     *
     * @see #PrometheusScraper(File, PrometheusDataFormat)
     *
     * @param url the URL where the Prometheus metric data is found
     * @param dataFormat the data format of the metric data found at the URL, or null if
     *                   the URL endpoint can provide it for us via content negotiation.
     * @param authorization the <code>Authorization</code> header value, or null
     */
    public PrometheusScraper(@NotNull URL url, @Nullable PrometheusDataFormat dataFormat, @Nullable String authorization) {
        this.url = url;
        this.knownDataFormat = dataFormat;
        this.authorization = authorization;
        log.debugf("Will scrape Prometheus data from URL [%s] with data format [%s]",
                this.url, this.knownDataFormat == null ? "<TBD>" : this.knownDataFormat);
    }

    /**
     * Scrape data from the given file. The data format will indicate if it
     * is binary protocol buffer data or text data ("text/plain").
     *
     * @param file the file to scrape
     * @param dataFormat the format of the metric data in the file.
     */
    public PrometheusScraper(@NotNull File file, @NotNull PrometheusDataFormat dataFormat) {
        try {
            this.url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("File does not have valid URL: " + file);
        }

        this.knownDataFormat = dataFormat;
        this.authorization = null;

        log.debugf("Will scrape Prometheus data from file [%s] with data format [%s]", this.url, this.knownDataFormat);
    }

    /**
     * This will collect all metric data from the endpoint and
     * return the entire list of all metric families found there.
     * @return all metric data found at the endpoint
     * @throws IOException if failed to scrape data
     */
    public @NotNull List<@NotNull MetricFamily> scrape() throws IOException {
        CollectorPrometheusMetricsWalker collector = new CollectorPrometheusMetricsWalker();
        scrape(collector);
        return Objects.requireNonNull(collector.getAllMetricFamilies(), "collector.getAllMetricFamilies() cannot be null");
    }

    public void scrape(@NotNull PrometheusMetricsWalker walker) throws IOException {
        OpenConnectionDetails connectionDetails = openConnection(this.url);
        try (InputStream inputStream = connectionDetails.getInputStream()) {
            String contentType = connectionDetails.getContentType();

            // if we were given a content type - we use it always. If we were not given a content type,
            // then use the one given to the constructor (if one was given).
            if ((contentType == null || contentType.contains("unknown"))) {
                contentType = this.knownDataFormat == null ? "text/plain" : this.knownDataFormat.getContentType();
            }

            PrometheusMetricsProcessor<?> processor;
            if (contentType.contains("application/vnd.google.protobuf")) {
                processor = new BinaryPrometheusMetricsProcessor(inputStream, walker);
            } else if (contentType.contains("text/plain")) {
                processor = new TextPrometheusMetricsProcessor(inputStream, walker);
            } else {
                // unknown - since all Prometheus endpoints are required to support text, try it
                log.debugf("Unknown content type for URL [%s]. Trying text format.", url);
                processor = new TextPrometheusMetricsProcessor(inputStream, walker);
            }

            processor.walk();
        }
    }

    /**
     * This is the content type of the supported Prometheus binary format.
     * This can be used in the Accept header when making the HTTP request to the Prometheus endpoint.
     *
     * @return binary format content type
     */
    @Contract(pure = true)
    protected @NotNull String getBinaryFormatContentType() {
        return PrometheusDataFormat.BINARY.getContentType();
    }

    /**
     * This is the content type of the supported Prometheus text format.
     * This can be used in the Accept header when making the HTTP request to the Prometheus endpoint.
     *
     * @return text format content type
     */
    @Contract(pure = true)
    protected @NotNull String getTextFormatContentType() {
        return PrometheusDataFormat.TEXT.getContentType();
    }

    /**
     * This provides a hook for subclasses to be able to connect to the Prometheus endpoint
     * and tell us what the content type is and to give us the actual stream to the data.
     * <p>
     * This is useful in case callers need to connect securely to the Prometheus endpoint.
     * Subclasses need to open the secure connection with their specific secure credentials
     * and other security details and return the input stream to the data (as well as its content type).
     * <p>
     * If subclasses return a null content type in the returned object the data format passed to this
     * object's constructor will be assumed as the data format in the input stream.
     * <p>
     * The default implementation is to simply open an unsecured connection to the URL.
     *
     * @param endpointUrl the Prometheus endpoint
     * @return connection details for the Prometheus endpoint
     *
     * @throws IOException if the connection could not be opened
     */
    @Contract("_ -> new")
    protected @NotNull OpenConnectionDetails openConnection(@NotNull URL endpointUrl) throws IOException {
        URLConnection conn = endpointUrl.openConnection();
        conn.setRequestProperty("Accept", getBinaryFormatContentType());
        if (this.authorization != null) conn.setRequestProperty("Authorization", this.authorization);
        InputStream stream = conn.getInputStream();
        String contentType = conn.getContentType();
        return new OpenConnectionDetails(stream, contentType);
    }
}

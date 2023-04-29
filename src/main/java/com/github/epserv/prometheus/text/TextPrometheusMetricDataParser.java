package com.github.epserv.prometheus.text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.epserv.prometheus.PrometheusMetricDataParser;
import com.github.epserv.prometheus.Util;
import org.jboss.logging.Logger;
import com.github.epserv.prometheus.types.Counter;
import com.github.epserv.prometheus.types.Gauge;
import com.github.epserv.prometheus.types.Histogram;
import com.github.epserv.prometheus.types.Metric;
import com.github.epserv.prometheus.types.MetricFamily;
import com.github.epserv.prometheus.types.MetricType;
import com.github.epserv.prometheus.types.Summary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a method that can scrape Prometheus text metric data from input streams.
 */
public class TextPrometheusMetricDataParser extends PrometheusMetricDataParser<MetricFamily> {
    private static final Logger log = Logger.getLogger(TextPrometheusMetricDataParser.class);

    private String lastLineReadFromStream; // this is only set when we break from the while loop in parse()

    /**
     * Provides the input stream where the parser will look for metric data.
     * NOTE: this object will not own this stream - it should never attempt to close it.
     *
     * @param inputStream the stream where the metric data can be found
     */
    public TextPrometheusMetricDataParser(@NotNull InputStream inputStream) {
        super(inputStream);
    }

    private static class ParserContext {
        // this is the metric family that has been fully built
        public MetricFamily finishedMetricFamily;

        // these are used when building a metric family
        public String name = "";
        public String help = "";
        public MetricType type = null;
        public final List<String> allowedNames = new ArrayList<>();
        public final List<TextSample> textSamples = new ArrayList<>();

        // starts a fresh metric family
        public void clear() {
            name = "";
            help = "";
            type = null;
            allowedNames.clear();
            textSamples.clear();
        }

        // complete the construction of the metric family
        public void finishMetricFamily() {
            if (finishedMetricFamily != null) {
                return;
            }

            MetricFamily.Builder metricFamilyBuilder = new MetricFamily.Builder();
            metricFamilyBuilder.setName(name);
            metricFamilyBuilder.setHelp(help);
            metricFamilyBuilder.setType(type);

            // need to convert the samples to metrics
            // We know if the family is a counter or a gauge, all samples are full metrics,
            // so we can convert them easily one-for-one.
            // For summary metrics, we need to combine all quantile samples, sum, and count.
            // For histogram metrics, we need to combine all bucket samples, sum, and count.

            Map<Map<String, String>, Metric.Builder<?, ?>> builders = new LinkedHashMap<>();

            for (TextSample textSample : textSamples) {
                try {
                    switch (type) {
                        case COUNTER -> builders.put(textSample.getLabels(),
                                new Counter.Builder().setName(name)
                                        .setValue(Util.convertStringToDouble(textSample.getValue()))
                                        .addLabels(textSample.getLabels()));
                        case GAUGE -> builders.put(textSample.getLabels(),
                                new Gauge.Builder().setName(name)
                                        .setValue(Util.convertStringToDouble(textSample.getValue()))
                                        .addLabels(textSample.getLabels()));
                        case SUMMARY -> {
                            // Get the builder that we are using to build up the current metric. Remember we need to
                            // get the builder for this specific metric identified with a unique set of labels.

                            // First we need to remove any existing quantile label since it isn't a "real" label.
                            // This is to ensure our lookup uses all but only "real" labels.
                            String quantileValue = textSample.getLabels().remove("quantile"); // may be null
                            Summary.Builder sBuilder = (Summary.Builder) builders.get(textSample.getLabels());
                            if (sBuilder == null) {
                                sBuilder = new Summary.Builder();
                                builders.put(textSample.getLabels(), sBuilder);
                            }
                            sBuilder.setName(name);
                            sBuilder.addLabels(textSample.getLabels());
                            if (textSample.getName().endsWith("_count")) {
                                sBuilder.setSampleCount((long) Util.convertStringToDouble(textSample.getValue()));
                            } else if (textSample.getName().endsWith("_sum")) {
                                sBuilder.setSampleSum(Util.convertStringToDouble(textSample.getValue()));
                            } else {
                                // This must be a quantile sample
                                if (quantileValue == null) {
                                    log.debugf("Summary quantile sample is missing the 'quantile' label: %s",
                                            textSample.getLine());
                                } else {
                                    sBuilder.addQuantile(
                                            Util.convertStringToDouble(quantileValue),
                                            Util.convertStringToDouble(textSample.getValue())
                                    );
                                }
                            }
                        }
                        case HISTOGRAM -> {
                            // Get the builder that we are using to build up the current metric. Remember we need to
                            // get the builder for this specific metric identified with a unique set of labels.

                            // First we need to remove any existing le label since it isn't a "real" label.
                            // This is to ensure our lookup uses all but only "real" labels.
                            String bucket = textSample.getLabels().remove("le"); // may be null
                            Histogram.Builder hBuilder = (Histogram.Builder) builders.get(textSample.getLabels());
                            if (hBuilder == null) {
                                hBuilder = new Histogram.Builder();
                                builders.put(textSample.getLabels(), hBuilder);
                            }
                            hBuilder.setName(name);
                            hBuilder.addLabels(textSample.getLabels());
                            if (textSample.getName().endsWith("_count")) {
                                hBuilder.setSampleCount((long) Util.convertStringToDouble(textSample.getValue()));
                            } else if (textSample.getName().endsWith("_sum")) {
                                hBuilder.setSampleSum(Util.convertStringToDouble(textSample.getValue()));
                            } else {
                                // This must be a bucket sample
                                if (bucket == null) {
                                    throw new Exception("Histogram bucket sample is missing the 'le' label");
                                }
                                hBuilder.addBucket(Util.convertStringToDouble(bucket),
                                        (long) Util.convertStringToDouble(textSample.getValue()));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debugf(e, "Error processing sample. This metric sample will be ignored: %s",
                            textSample.getLine());
                }
            }

            // now that we've combined everything into individual metric builders, we can build all our metrics
            for (Metric.Builder<?, ?> builder : builders.values()) {
                try {
                    metricFamilyBuilder.addMetric(builder.build());
                } catch (Exception e) {
                    log.debugf(e, "Error building metric for metric family [%s] - it will be ignored", name);
                }
            }

            finishedMetricFamily = metricFamilyBuilder.build();
        }
    }

    @Override
    public MetricFamily parse() throws IOException {

        // determine the first line we should process. If we were previously called, we already
        // read a line - start from that last line read. Otherwise, prime the pump and read
        // the first line from the stream.
        String line;
        if (lastLineReadFromStream != null) {
            line = lastLineReadFromStream;
            lastLineReadFromStream = null;
        } else {
            line = readLine(getInputStream());
        }

        if (line == null) {
            return null;
        }

        // do a quick check to see if we are getting passed in binary format rather than text
        if (!line.isEmpty() && !String.valueOf(line.charAt(0)).matches("\\p{ASCII}*")) {
            throw new IOException("Doesn't look like the metric data is in text format");
        }

        ParserContext context = new ParserContext();

        while (line != null) {
            line = line.trim();

            try {
                if (!line.isEmpty()) {
                    if (line.charAt(0) == '#') {
                        String[] parts = line.split("[ \t]+", 4); // 0 is #, 1 is HELP or TYPE, 2 is metric name, 3 is doc
                        if (parts.length >= 2) {
                            if (parts[1].equals("HELP")) {
                                if (!parts[2].equals(context.name)) {
                                    // we are hitting a new metric family
                                    if (!context.name.isEmpty()) {
                                        // break and we'll finish the metric family we previously were building up
                                        this.lastLineReadFromStream = line;
                                        break;
                                    }
                                    // start anew
                                    context.clear();
                                    context.name = parts[2];
                                    context.type = MetricType.GAUGE; // default in case we don't get a TYPE
                                    context.allowedNames.add(parts[2]);
                                }

                                if (parts.length == 4) {
                                    context.help = unescapeHelp(parts[3]);
                                } else {
                                    context.help = "";
                                }
                            } else if (parts[1].equals("TYPE")) {
                                if (!parts[2].equals(context.name)) {
                                    if (!context.name.isEmpty()) {
                                        // break and we'll finish the metric family we previously were building up
                                        this.lastLineReadFromStream = line;
                                        break;
                                    }
                                    // start anew
                                    context.clear();
                                    context.name = parts[2];
                                }
                                context.type = MetricType.valueOf(parts[3].toUpperCase());
                                context.allowedNames.clear();
                                switch (context.type) {
                                    case COUNTER, GAUGE -> context.allowedNames.add(context.name);
                                    case SUMMARY -> {
                                        context.allowedNames.add(context.name + "_count");
                                        context.allowedNames.add(context.name + "_sum");
                                        context.allowedNames.add(context.name);
                                    }
                                    case HISTOGRAM -> {
                                        context.allowedNames.add(context.name + "_count");
                                        context.allowedNames.add(context.name + "_sum");
                                        context.allowedNames.add(context.name + "_bucket");
                                    }
                                }
                            }
                        }
                    } else {
                        // parse the sample line that contains a single metric (or part of a metric as in summary/histo)
                        TextSample sample = parseSampleLine(line);
                        if (!context.allowedNames.contains(sample.getName())) {
                            if (!context.name.isEmpty()) {
                                // break and we'll finish the metric family we previously were building up
                                this.lastLineReadFromStream = line;
                                break;
                            }
                            context.clear();
                            log.debugf("Ignoring an unexpected metric: " + line);
                        } else {
                            // add the sample to the family we are building up
                            context.textSamples.add(sample);
                        }
                    }
                }
            } catch (Exception e) {
                log.debugf("Failed to process line - it will be ignored: %s", line);
            }

            // go to the next line
            line = readLine(getInputStream());
        }

        if (!context.name.isEmpty()) {
            // finish the metric family we previously were building up
            context.finishMetricFamily();
        }

        return context.finishedMetricFamily;
    }

    private TextSample parseSampleLine(String line) {
        // algorithm from parser.py
        StringBuilder name = new StringBuilder();
        StringBuilder labelname = new StringBuilder();
        StringBuilder labelvalue = new StringBuilder();
        StringBuilder value = new StringBuilder();
        Map<String, String> labels = new LinkedHashMap<>();

        String state = "name";

        label:
        for (int c = 0; c < line.length(); c++) {
            char charAt = line.charAt(c);
            switch (state) {
                case "name" -> {
                    if (charAt == '{') {
                        state = "startoflabelname";
                    } else if (charAt == ' ' || charAt == '\t') {
                        state = "endofname";
                    } else {
                        name.append(charAt);
                    }
                }
                case "endofname" -> {
                    if (charAt != ' ' && charAt != '\t') {
                        if (charAt == '{') {
                            state = "startoflabelname";
                        } else {
                            value.append(charAt);
                            state = "value";
                        }
                    }
                }
                case "startoflabelname" -> {
                    if (charAt == '}') {
                        state = "endoflabels";
                    } else if (charAt != ' ' && charAt != '\t') {
                        labelname.append(charAt);
                        state = "labelname";
                    }
                }
                case "labelname" -> {
                    if (charAt == '=') {
                        state = "labelvaluequote";
                    } else if (charAt == '}') {
                        state = "endoflabels";
                    } else if (charAt == ' ' || charAt == '\t') {
                        state = "labelvalueequals";
                    } else {
                        labelname.append(charAt);
                    }
                }
                case "labelvalueequals" -> {
                    if (charAt == '=') {
                        state = "labelvaluequote";
                    } else if (charAt != ' ' && charAt != '\t') {
                        throw new IllegalStateException("Invalid line: " + line);
                    }
                }
                case "labelvaluequote" -> {
                    if (charAt == '"') {
                        state = "labelvalue";
                    } else if (charAt != ' ' && charAt != '\t') {
                        throw new IllegalStateException("Invalid line: " + line);
                    }
                }
                case "labelvalue" -> {
                    if (charAt == '\\') {
                        state = "labelvalueslash";
                    } else if (charAt == '"') {
                        labels.put(labelname.toString(), labelvalue.toString());
                        labelname.setLength(0);
                        labelvalue.setLength(0);
                        state = "nextlabel";
                    } else {
                        labelvalue.append(charAt);
                    }
                }
                case "labelvalueslash" -> {
                    state = "labelvalue";
                    if (charAt == '\\') {
                        labelvalue.append('\\');
                    } else if (charAt == 'n') {
                        labelvalue.append('\n');
                    } else if (charAt == '"') {
                        labelvalue.append('"');
                    } else {
                        labelvalue.append('\\').append(charAt);
                    }
                }
                case "nextlabel" -> {
                    if (charAt == ',') {
                        state = "labelname";
                    } else if (charAt == '}') {
                        state = "endoflabels";
                    } else if (charAt != ' ' && charAt != '\t') {
                        throw new IllegalStateException("Invalid line: " + line);
                    }
                }
                case "endoflabels" -> {
                    if (charAt != ' ' && charAt != '\t') {
                        value.append(charAt);
                        state = "value";
                    }
                }
                case "value" -> {
                    if (charAt == ' ' || charAt == '\t') {
                        break label; // timestamps are NOT supported - ignoring
                    } else {
                        value.append(charAt);
                    }
                }
            }
        }

        return new TextSample.Builder()
                .setLine(line)
                .setName(name.toString())
                .setValue(value.toString())
                .addLabels(labels).build();
    }

    private String unescapeHelp(String text) {
        // algorithm from parser.py
        if (text == null || !text.contains("\\")) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        boolean slash = false;
        for (int c = 0; c < text.length(); c++) {
            char charAt = text.charAt(c);
            if (slash) {
                if (charAt == '\\') {
                    result.append('\\');
                } else if (charAt == 'n') {
                    result.append('\n');
                } else {
                    result.append('\\').append(charAt);
                }
                slash = false;
            } else {
                if (charAt == '\\') {
                    slash = true;
                } else {
                    result.append(charAt);
                }
            }
        }
        if (slash) {
            result.append("\\");
        }
        return result.toString();
    }

    private @Nullable String readLine(@NotNull InputStream inputStream) throws IOException {
        int lineChar;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Prometheus end of line character is a newline
        for (lineChar = inputStream.read(); (lineChar != '\n' && lineChar != -1); lineChar = inputStream.read()) {
            baos.write(lineChar);
        }

        if (lineChar == -1 && baos.size() == 0) {
            // EOF
            return null;
        }

        return baos.toString(StandardCharsets.UTF_8);
    }

}

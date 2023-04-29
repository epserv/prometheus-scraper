package com.github.epserv.prometheus;

import java.net.URL;

import org.jboss.logging.Logger.Level;
import com.github.epserv.prometheus.walkers.JSONPrometheusMetricsWalker;
import com.github.epserv.prometheus.walkers.LoggingPrometheusMetricsWalker;
import com.github.epserv.prometheus.walkers.PrometheusMetricsWalker;
import com.github.epserv.prometheus.walkers.SimplePrometheusMetricsWalker;
import com.github.epserv.prometheus.walkers.XMLPrometheusMetricsWalker;

/**
 * This is a command line utility that can scrape a Prometheus protocol endpoint and outputs the metric data it finds.
 * You provide a single required argument on the command line - the URL of the Prometheus protocol endpoint, which is
 * typically something like <code>http://localhost:9090/metrics</code>.
 */
public class PrometheusScraperCli {

    enum PrometheusMetricsWalkerType {
        LOG, SIMPLE, XML, JSON
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new Exception("Specify the URL of the Prometheus protocol endpoint.");
        }

        PrometheusMetricsWalkerType walkerType = PrometheusMetricsWalkerType.SIMPLE;
        URL url = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equalsIgnoreCase("--xml")) {
                    walkerType = PrometheusMetricsWalkerType.XML;
                } else if (arg.equalsIgnoreCase("--json")) {
                    walkerType = PrometheusMetricsWalkerType.JSON;
                } else if (arg.equalsIgnoreCase("--simple")) {
                    walkerType = PrometheusMetricsWalkerType.SIMPLE;
                } else if (arg.equalsIgnoreCase("--log")) {
                    walkerType = PrometheusMetricsWalkerType.LOG;
                } else {
                    throw new Exception("Invalid argument: " + arg);
                }
            } else {
                url = new URL(arg);
                break;
            }
        }

        if (url == null) {
            throw new Exception("Specify the URL of the Prometheus protocol endpoint.");
        }

        PrometheusMetricsWalker walker = switch (walkerType) {
            case SIMPLE -> new SimplePrometheusMetricsWalker(url);
            case XML -> new XMLPrometheusMetricsWalker(url);
            case JSON -> new JSONPrometheusMetricsWalker();
            case LOG -> new LoggingPrometheusMetricsWalker(Level.INFO);
        };

        PrometheusScraper scraper = new PrometheusScraper(url);
        scraper.scrape(walker);
    }

}

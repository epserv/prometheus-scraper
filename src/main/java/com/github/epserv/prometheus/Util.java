package com.github.epserv.prometheus;

import org.jetbrains.annotations.NotNull;

public class Util {

    public static double convertStringToDouble(@NotNull String valueString) {
        double doubleValue;
        if (valueString.equalsIgnoreCase("NaN")) {
            doubleValue = Double.NaN;
        } else if (valueString.equalsIgnoreCase("+Inf")) {
            doubleValue = Double.POSITIVE_INFINITY;
        } else if (valueString.equalsIgnoreCase("-Inf")) {
            doubleValue = Double.NEGATIVE_INFINITY;
        } else {
            doubleValue = Double.parseDouble(valueString);
        }
        return doubleValue;
    }

    public static String convertDoubleToString(double value) {
        // Prometheus spec requires positive infinity to be denoted as "+Inf" and negative infinity as "-Inf"
        if (Double.isInfinite(value)) {
            return (value < 0.0) ? "-Inf" : "+Inf";
        }
        return String.format("%f", value);
    }
}

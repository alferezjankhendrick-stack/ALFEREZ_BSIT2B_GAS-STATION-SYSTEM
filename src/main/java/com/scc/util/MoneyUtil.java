package com.scc.util;

public final class MoneyUtil {
    private MoneyUtil() {
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String peso(double value) {
        return String.format("P%,.2f", value);
    }
}
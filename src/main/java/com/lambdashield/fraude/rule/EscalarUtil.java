package com.lambdashield.fraude.rule;

public final class EscalarUtil {
    private EscalarUtil() {}

    public static double escalar(double valor, double minIn, double maxIn, double minOut, double maxOut) {
        if (maxIn == minIn) return minOut;
        double t = (valor - minIn) / (maxIn - minIn);
        double limitado = Math.max(0.0, Math.min(1.0, t));
        return minOut + limitado * (maxOut - minOut);
    }
}

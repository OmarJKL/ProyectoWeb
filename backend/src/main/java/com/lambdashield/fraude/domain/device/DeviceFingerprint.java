package com.lambdashield.fraude.domain.device;

import java.util.Locale;

/** Funciones puras para normalizar la huella del dispositivo declarada por el navegador. */
public final class DeviceFingerprint {

    private DeviceFingerprint() {}

    public static String normalize(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Desconocido|Desconocido";

        String ua = userAgent.toLowerCase(Locale.ROOT);
        String browser = ua.contains("edg/") ? "Edge"
                : ua.contains("firefox/") ? "Firefox"
                : ua.contains("chrome/") || ua.contains("crios/") ? "Chrome"
                : ua.contains("safari/") ? "Safari"
                : "Otro";
        String os = ua.contains("android") ? "Android"
                : ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios") ? "iOS"
                : ua.contains("windows") ? "Windows"
                : ua.contains("mac os") || ua.contains("macintosh") ? "macOS"
                : ua.contains("linux") ? "Linux"
                : "Otro";
        return browser + "|" + os;
    }

    public static String from(String codigoCliente, String dispositivoNormalizado) {
        return codigoCliente + "|" + dispositivoNormalizado;
    }
}

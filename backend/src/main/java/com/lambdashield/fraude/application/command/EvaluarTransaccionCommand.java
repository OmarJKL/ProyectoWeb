package com.lambdashield.fraude.application.command;

/** Comando interno de aplicación para evaluar una operación; no depende del formato HTTP/JSON. */
public record EvaluarTransaccionCommand(
        String cliente,
        double monto,
        String categoria,
        int hora,
        String pais,
        String dispositivo,
        int operacionesUltimaHora,
        String beneficiario,
        String ip
) {}

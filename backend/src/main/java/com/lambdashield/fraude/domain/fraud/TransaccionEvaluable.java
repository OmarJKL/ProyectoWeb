package com.lambdashield.fraude.domain.fraud;

/** Datos de una transacción que necesita el motor de reglas, sin acoplamiento a HTTP. */
public record TransaccionEvaluable(
        double monto,
        int hora,
        String pais,
        String dispositivo,
        int operacionesUltimaHora,
        String beneficiario,
        String ip
) {}

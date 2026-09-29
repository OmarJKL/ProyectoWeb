package com.lambdashield.fraude.dto.request;

public record TransaccionCrearRequest(
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

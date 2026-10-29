package com.lambdashield.fraude.dto.response;

public record FactorResponse(
    String id,
    String etiqueta,
    int puntaje,
    int max,
    String motivo
) {}

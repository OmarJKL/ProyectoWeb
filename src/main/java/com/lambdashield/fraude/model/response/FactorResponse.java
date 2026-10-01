package com.lambdashield.fraude.model.response;

public record FactorResponse(
    String id,
    String etiqueta,
    int puntaje,
    int max,
    String motivo
) {}

package com.lambdashield.fraude.presentation.dto.response;

public record FactorResponse(
    String id,
    String etiqueta,
    int puntaje,
    int max,
    String motivo
) {}

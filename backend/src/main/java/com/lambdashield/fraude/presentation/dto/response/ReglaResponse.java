package com.lambdashield.fraude.presentation.dto.response;

public record ReglaResponse(
    String codigo,
    String nombre,
    String descripcion,
    int pesoMaximo
) {}

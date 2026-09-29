package com.lambdashield.fraude.dto.response;

public record ReglaResponse(
    String codigo,
    String nombre,
    String descripcion,
    int pesoMaximo
) {}

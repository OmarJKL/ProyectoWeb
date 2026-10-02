package com.lambdashield.fraude.model.response;

public record ReglaResponse(
    String codigo,
    String nombre,
    String descripcion,
    int pesoMaximo
) {}

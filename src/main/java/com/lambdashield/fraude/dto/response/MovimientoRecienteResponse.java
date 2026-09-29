package com.lambdashield.fraude.dto.response;

public record MovimientoRecienteResponse(
    String fecha,
    String descripcion,
    boolean esIngreso,
    int monto
) {}

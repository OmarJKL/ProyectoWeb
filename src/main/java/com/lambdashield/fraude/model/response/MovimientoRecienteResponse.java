package com.lambdashield.fraude.model.response;

public record MovimientoRecienteResponse(
    String fecha,
    String descripcion,
    boolean esIngreso,
    int monto
) {}

package com.lambdashield.fraude.presentation.dto.response;

public record MovimientoRecienteResponse(
    String fecha,
    String descripcion,
    boolean esIngreso,
    int monto
) {}

package com.lambdashield.fraude.dto.response;

public record MovimientoDiarioResponse(
    String fecha,
    int ingreso,
    int egreso
) {}

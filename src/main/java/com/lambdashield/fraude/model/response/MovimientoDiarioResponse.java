package com.lambdashield.fraude.model.response;

public record MovimientoDiarioResponse(
    String fecha,
    int ingreso,
    int egreso
) {}

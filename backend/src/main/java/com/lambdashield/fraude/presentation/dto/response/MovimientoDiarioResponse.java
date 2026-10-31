package com.lambdashield.fraude.presentation.dto.response;

public record MovimientoDiarioResponse(
    String fecha,
    int ingreso,
    int egreso
) {}

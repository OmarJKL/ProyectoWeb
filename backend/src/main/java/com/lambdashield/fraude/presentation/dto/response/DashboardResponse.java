package com.lambdashield.fraude.presentation.dto.response;

public record DashboardResponse(
    double saldo,
    double ingresos,
    double egresos,
    double variacionSaldo,
    double variacionIngresos,
    double variacionEgresos
) {}

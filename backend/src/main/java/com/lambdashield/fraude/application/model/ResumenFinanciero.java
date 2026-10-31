package com.lambdashield.fraude.application.model;

/** Balance del periodo consultado con sus variaciones porcentuales. */
public record ResumenFinanciero(
        double saldo,
        double ingresos,
        double egresos,
        double variacionSaldo,
        double variacionIngresos,
        double variacionEgresos
) {}
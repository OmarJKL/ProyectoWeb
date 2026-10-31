package com.lambdashield.fraude.application.model;

/** Serie diaria de ingresos y egresos para gráficos. */
public record MovimientoDiario(String fecha, int ingreso, int egreso) {}
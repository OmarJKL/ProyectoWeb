package com.lambdashield.fraude.application.model;

/** Movimiento listado en la tabla de actividad reciente. */
public record MovimientoReciente(String fecha, String descripcion, boolean esIngreso, int monto) {}
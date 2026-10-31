package com.lambdashield.fraude.application.model;

/** Indicadores agregados del historial de evaluaciones activas. */
public record KpisResultado(
        long total,
        long aprobadas,
        long monitoreadas,
        long enRevision,
        long bloqueadas,
        double puntajePromedio
) {}
package com.lambdashield.fraude.model.response;

public record KpisResponse(
    long total,
    long aprobadas,
    long monitoreadas,
    long enRevision,
    long bloqueadas,
    double puntajePromedio
) {}

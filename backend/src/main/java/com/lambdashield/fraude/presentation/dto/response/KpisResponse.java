package com.lambdashield.fraude.presentation.dto.response;

public record KpisResponse(
    long total,
    long aprobadas,
    long monitoreadas,
    long enRevision,
    long bloqueadas,
    double puntajePromedio
) {}

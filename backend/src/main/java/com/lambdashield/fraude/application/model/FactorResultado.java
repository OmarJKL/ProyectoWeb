package com.lambdashield.fraude.application.model;

/** Puntaje de una regla dentro de una evaluación, expresado en lenguaje de aplicación. */
public record FactorResultado(String id, String etiqueta, int puntaje, int max, String motivo) {}

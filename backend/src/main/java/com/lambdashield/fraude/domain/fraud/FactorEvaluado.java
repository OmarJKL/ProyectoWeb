package com.lambdashield.fraude.domain.fraud;

/** Resultado de aplicar una regla antifraude a una transacción. */
public record FactorEvaluado(String id, String etiqueta, int puntaje, int max, String motivo) {}

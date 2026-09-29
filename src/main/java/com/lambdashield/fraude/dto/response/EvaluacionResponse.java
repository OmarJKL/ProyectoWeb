package com.lambdashield.fraude.dto.response;

import java.util.List;

public record EvaluacionResponse(
    String id,
    String clienteNombre,
    double monto,
    String categoria,
    String pais,
    int hora,
    int puntaje,
    String nivel,
    String estado,
    String iconoEstado,
    String colorNivel,
    String fecha,
    List<FactorResponse> factores
) {}

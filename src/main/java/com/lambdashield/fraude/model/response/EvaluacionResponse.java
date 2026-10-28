package com.lambdashield.fraude.model.response;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluacionResponse(
    String id,
    String clienteCodigo,
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
    LocalDateTime fechaHora,
    List<FactorResponse> factores,
    Long idInterno
) {}

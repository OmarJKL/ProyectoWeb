package com.lambdashield.fraude.model.response;

public record TransaccionResponse(
    String id,
    String clienteNombre,
    double monto,
    String categoria,
    String categoriaDisplay,
    String pais,
    String paisDisplay,
    int hora,
    int puntaje,
    String nivel,
    String estado,
    String iconoEstado,
    String colorNivel,
    String fecha
) {}

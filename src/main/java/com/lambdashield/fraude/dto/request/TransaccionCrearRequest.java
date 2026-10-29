package com.lambdashield.fraude.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransaccionCrearRequest(
    @NotBlank(message = "El cliente es obligatorio")
    String cliente,

    @Positive(message = "El monto debe ser mayor que cero")
    double monto,

    String categoria,

    @Min(value = 0, message = "La hora debe estar entre 0 y 23")
    @Max(value = 23, message = "La hora debe estar entre 0 y 23")
    int hora,

    @NotBlank(message = "El país es obligatorio")
    @Pattern(regexp = "local|regional|alto_riesgo",
            message = "País inválido (permitidos: local, regional, alto_riesgo)")
    String pais,

    @NotBlank(message = "El dispositivo es obligatorio")
    @Pattern(regexp = "reconocido|nuevo",
            message = "Dispositivo inválido (permitidos: reconocido, nuevo)")
    String dispositivo,

    @Min(value = 0, message = "Las operaciones de la última hora no pueden ser negativas")
    int operacionesUltimaHora,

    @NotBlank(message = "El beneficiario es obligatorio")
    @Pattern(regexp = "recurrente|nuevo",
            message = "Beneficiario inválido (permitidos: recurrente, nuevo)")
    String beneficiario,

    @NotBlank(message = "La IP es obligatoria")
    @Pattern(regexp = "conocida|nueva",
            message = "IP inválida (permitidos: conocida, nueva)")
    String ip
) {}

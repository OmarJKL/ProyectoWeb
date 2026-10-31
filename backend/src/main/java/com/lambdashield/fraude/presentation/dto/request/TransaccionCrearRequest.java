package com.lambdashield.fraude.presentation.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransaccionCrearRequest(
    @NotBlank(message = "El cliente es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9-]{1,20}",
            message = "El código de cliente admite hasta 20 letras, números o guiones")
    String cliente,

    @Positive(message = "El monto debe ser mayor que cero")
    @Digits(integer = 16, fraction = 2,
            message = "El monto admite hasta 16 enteros y 2 decimales")
    double monto,

    @Pattern(regexp = "[A-Za-z_]{0,40}",
            message = "La categoría solo admite letras y guion bajo (máximo 40 caracteres)")
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

    @Min(value = 0, message = "Las operaciones de la última hora deben estar entre 0 y 1000")
    @Max(value = 1000, message = "Las operaciones de la última hora deben estar entre 0 y 1000")
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

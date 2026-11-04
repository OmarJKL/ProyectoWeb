package com.lambdashield.fraude.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioClaveRequest(
    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
    String clave
) {}

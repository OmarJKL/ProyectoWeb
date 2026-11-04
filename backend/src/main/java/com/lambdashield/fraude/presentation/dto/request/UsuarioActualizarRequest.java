package com.lambdashield.fraude.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioActualizarRequest(
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 120, message = "Los nombres admiten hasta 120 caracteres")
    String nombres,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    String correo,

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "ACTIVO|BLOQUEADO|INACTIVO",
            message = "Estado inválido (permitidos: ACTIVO, BLOQUEADO, INACTIVO)")
    String estado,

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "ADMIN|ANALISTA|CLIENTE", message = "Rol inválido (permitidos: ADMIN, ANALISTA, CLIENTE)")
    String rol,

    String codigoCliente
) {}

package com.lambdashield.fraude.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Pattern(regexp = "[a-z0-9._-]{3,20}",
            message = "El usuario admite de 3 a 20 caracteres en minúscula: letras, números, punto, guion o guion bajo")
    String usuario,

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 120, message = "Los nombres admiten hasta 120 caracteres")
    String nombres,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    String correo,

    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
    String clave,

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "ADMIN|ANALISTA|CLIENTE", message = "Rol inválido (permitidos: ADMIN, ANALISTA, CLIENTE)")
    String rol,

    String codigoCliente
) {}

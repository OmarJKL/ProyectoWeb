package com.lambdashield.fraude.application.model;

import java.time.LocalDateTime;

/** Usuario del sistema expresado en lenguaje de aplicación, sin detalles de interfaz. */
public record UsuarioResumen(
        Long id,
        String usuario,
        String nombres,
        String correo,
        String estado,
        String rol,
        String codigoCliente,
        LocalDateTime ultimoAcceso
) {}

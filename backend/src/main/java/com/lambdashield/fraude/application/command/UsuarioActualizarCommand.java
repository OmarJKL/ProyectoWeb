package com.lambdashield.fraude.application.command;

/** Actualización de un usuario existente; el cambio de clave es un caso aparte. */
public record UsuarioActualizarCommand(
        String nombres,
        String correo,
        String estado,
        String rol,
        String codigoCliente
) {}

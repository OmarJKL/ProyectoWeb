package com.lambdashield.fraude.application.command;

/** Alta de un usuario del sistema; no depende del formato HTTP/JSON. */
public record UsuarioCrearCommand(
        String usuario,
        String nombres,
        String correo,
        String clave,
        String rol,
        String codigoCliente
) {}

package com.lambdashield.fraude.presentation.dto.response;

public record UsuarioResponse(
    Long id,
    String usuario,
    String nombres,
    String correo,
    String estado,
    String rol,
    String codigoCliente,
    String ultimoAcceso
) {}

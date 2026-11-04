package com.lambdashield.fraude.application.service;

/**
 * Se lanza cuando se busca un usuario del sistema que no existe.
 */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long idUsuario) {
        super("Usuario no encontrado: " + idUsuario);
    }
}

package com.lambdashield.fraude.application.service;

/**
 * Se lanza cuando se intenta evaluar una operación para un cliente que no existe.
 */
public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(String codigoCliente) {
        super("Cliente no encontrado: " + codigoCliente);
    }
}

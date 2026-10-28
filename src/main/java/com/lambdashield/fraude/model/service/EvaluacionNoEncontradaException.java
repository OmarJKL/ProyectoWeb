package com.lambdashield.fraude.model.service;

/**
 * Se lanza cuando se intenta desactivar una evaluación inexistente o ya inactiva.
 */
public class EvaluacionNoEncontradaException extends RuntimeException {

    public EvaluacionNoEncontradaException(Long idEvaluacion) {
        super("Evaluación no encontrada: " + idEvaluacion);
    }
}

package com.lambdashield.fraude.controller;

import com.lambdashield.fraude.model.service.ClienteNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manejo global de errores.
 *
 * Para la API responde siempre JSON con la forma {"errores":[...]}:
 *  - 400 para errores de validación
 *  - 404 para cliente inexistente
 * Para las páginas se conserva la vista error.html.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.badRequest().body(Map.of("errores", errores));
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, List<String>>> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("errores", List.of(mensajeSeguro(ex))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, List<String>>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("errores", List.of(mensajeSeguro(ex))));
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request, Model model) {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errores", List.of("Error interno del servidor")));
        }
        model.addAttribute("error", mensajeSeguro(ex));
        return "error";
    }

    private String mensajeSeguro(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Error inesperado";
    }
}

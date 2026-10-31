package com.lambdashield.fraude.presentation.exception;

import com.lambdashield.fraude.application.service.ClienteNoEncontradoException;
import com.lambdashield.fraude.application.service.EvaluacionNoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, List<String>>> handleMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("errores", List.of("Método no permitido; use PATCH para la baja lógica")));
    }

    @ExceptionHandler(EvaluacionNoEncontradaException.class)
    public ResponseEntity<Map<String, List<String>>> handleEvaluacionNoEncontrada(EvaluacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("errores", List.of(mensajeSeguro(ex))));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidacionParametros(HandlerMethodValidationException ex) {
        List<String> errores = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.badRequest().body(Map.of("errores", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("errores", List.of("El cuerpo de la petición es obligatorio y debe ser JSON válido")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("errores", List.of("El parámetro '" + ex.getName() + "' tiene un valor inválido")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, List<String>>> handleParametroAusente(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("errores", List.of("Falta el parámetro obligatorio: " + ex.getParameterName())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, List<String>>> handleTipoContenido(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("errores", List.of("Tipo de contenido no soportado; use application/json")));
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

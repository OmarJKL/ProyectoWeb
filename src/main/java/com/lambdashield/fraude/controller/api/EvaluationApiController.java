package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.service.TransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — CAPA CONTROLADOR
 *
 * Recurso: /api/v1/evaluation
 * Responsabilidad única: gestionar las evaluaciones antifraude.
 *
 * Cada método cumple exactamente una función:
 *   POST   → evaluar una transacción y guardarla
 *   GET    → listar todas las evaluaciones registradas
 *   DELETE → limpiar el historial de evaluaciones
 */
@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationApiController {

    private final TransaccionService transaccionService;

    public EvaluationApiController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    /** Evalúa una transacción con el motor antifraude y la registra. */
    @PostMapping
    public ResponseEntity<EvaluacionResponse> evaluate(@RequestBody TransaccionCrearRequest request) {
        return ResponseEntity.ok(transaccionService.crearYEvaluar(request));
    }

    /** Devuelve el historial completo de evaluaciones registradas. */
    @GetMapping
    public ResponseEntity<List<EvaluacionResponse>> list() {
        return ResponseEntity.ok(transaccionService.listarTodas());
    }

    /** Limpia el historial de evaluaciones (reinicia el store). */
    @DeleteMapping
    public ResponseEntity<Void> clear() {
        transaccionService.limpiarHistorial();
        return ResponseEntity.noContent().build();
    }
}

package com.lambdashield.fraude.presentation.api;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.service.TransaccionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller - CAPA PRESENTACIÓN / API
 *
 * Recurso: /api/v1/evaluation
 * Responsabilidad única: gestionar las evaluaciones antifraude.
 *
 *   POST        -> evaluar una transacción y guardarla
 *   GET         -> listar las evaluaciones activas registradas
 *   PATCH /{id} -> desactivar una evaluación (soft delete, sin borrar la fila)
 *
 * No existe DELETE: la baja lógica se realiza con PATCH /{id}.
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
    public ResponseEntity<EvaluacionResponse> evaluate(@Valid @RequestBody TransaccionCrearRequest request,
                                                       HttpSession session,
                                                       HttpServletRequest httpRequest) {
        // Un cliente no puede declarar sus propios factores: se derivan en el servidor.
        if ("CLIENTE".equals(session.getAttribute("rol"))) {
            String codigoCliente = (String) session.getAttribute("codigoCliente");
            if (codigoCliente == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(transaccionService.crearYEvaluarCliente(
                    request,
                    codigoCliente,
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr()));
        }

        // El simulador del administrador conserva los valores manuales para pruebas.
        return ResponseEntity.ok(transaccionService.crearYEvaluar(request));
    }

    /** Devuelve el historial de evaluaciones activas. */
    @GetMapping
    public ResponseEntity<List<EvaluacionResponse>> list() {
        return ResponseEntity.ok(transaccionService.listarTodas());
    }

    /**
     * Baja lógica de una evaluación.
     *
     * La fila no se elimina: se marca es_activa = false y la transacción
     * asociada pasa a REVERSADA.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        transaccionService.desactivarEvaluacion(id);
        return ResponseEntity.noContent().build();
    }
}

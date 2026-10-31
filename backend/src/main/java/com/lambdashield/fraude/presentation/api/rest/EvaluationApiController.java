package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.application.command.EvaluarTransaccionCommand;
import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.application.service.EvaluacionTransaccionService;
import com.lambdashield.fraude.presentation.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para /api/v1/evaluation: evaluar transacciones, listar el historial
 * y desactivar evaluaciones mediante baja lógica. No existe DELETE: se usa PATCH /{id}.
 */
@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationApiController {

    private final EvaluacionTransaccionService evaluacionTransaccionService;
    private final EvaluacionConsultaService evaluacionConsultaService;
    private final EvaluacionResponseMapper mapper;

    public EvaluationApiController(EvaluacionTransaccionService evaluacionTransaccionService,
                                   EvaluacionConsultaService evaluacionConsultaService,
                                   EvaluacionResponseMapper mapper) {
        this.evaluacionTransaccionService = evaluacionTransaccionService;
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.mapper = mapper;
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
            return ResponseEntity.ok(mapper.toResponse(evaluacionTransaccionService.crearYEvaluarCliente(
                    toCommand(request),
                    codigoCliente,
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr())));
        }

        // El simulador del administrador conserva los valores manuales para pruebas.
        return ResponseEntity.ok(mapper.toResponse(evaluacionTransaccionService.crearYEvaluar(toCommand(request))));
    }

    /** Devuelve el historial de evaluaciones activas. */
    @GetMapping
    public ResponseEntity<List<EvaluacionResponse>> list() {
        return ResponseEntity.ok(mapper.toResponses(evaluacionConsultaService.listarTodas()));
    }

    /**
     * Baja lógica de una evaluación: la fila no se elimina, se marca es_activa = false
     * y la transacción asociada pasa a REVERSADA.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable @Positive(message = "El identificador debe ser positivo") Long id) {
        evaluacionTransaccionService.desactivarEvaluacion(id);
        return ResponseEntity.noContent().build();
    }

    private static EvaluarTransaccionCommand toCommand(TransaccionCrearRequest request) {
        return new EvaluarTransaccionCommand(
                request.cliente(), request.monto(), request.categoria(), request.hora(), request.pais(),
                request.dispositivo(), request.operacionesUltimaHora(), request.beneficiario(), request.ip());
    }
}

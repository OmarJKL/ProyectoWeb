package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.dto.response.KpisResponse;
import com.lambdashield.fraude.service.TransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transacciones")
public class TransaccionApiController {

    private final TransaccionService transaccionService;

    public TransaccionApiController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @PostMapping
    public ResponseEntity<EvaluacionResponse> crear(@RequestBody TransaccionCrearRequest request) {
        return ResponseEntity.ok(transaccionService.crearYEvaluar(request));
    }

    @GetMapping
    public ResponseEntity<List<EvaluacionResponse>> listar() {
        return ResponseEntity.ok(transaccionService.listarTodas());
    }

    @GetMapping("/kpis")
    public ResponseEntity<KpisResponse> kpis() {
        return ResponseEntity.ok(transaccionService.calcularKpis());
    }

    @DeleteMapping
    public ResponseEntity<Void> limpiar() {
        transaccionService.limpiarHistorial();
        return ResponseEntity.noContent().build();
    }
}

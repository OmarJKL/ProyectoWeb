package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.model.response.ReglaResponse;
import com.lambdashield.fraude.model.service.ReglaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller - CAPA CONTROLADOR
 *
 * Recurso: /api/v1/rule
 * Responsabilidad única: exponer las reglas activas del motor antifraude.
 * Las reglas se leen desde la tabla regla_fraude.
 */
@RestController
@RequestMapping("/api/v1/rule")
public class RuleApiController {

    private final ReglaService reglaService;

    public RuleApiController(ReglaService reglaService) {
        this.reglaService = reglaService;
    }

    /** Devuelve la lista de reglas activas almacenadas en la base de datos. */
    @GetMapping
    public ResponseEntity<List<ReglaResponse>> list() {
        return ResponseEntity.ok(reglaService.listar());
    }
}

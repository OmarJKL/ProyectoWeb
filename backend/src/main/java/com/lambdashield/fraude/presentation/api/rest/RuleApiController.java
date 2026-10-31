package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.application.service.ReglaConsultaService;
import com.lambdashield.fraude.presentation.dto.response.ReglaResponse;
import com.lambdashield.fraude.presentation.mapper.ReglaResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para /api/v1/rule: expone las reglas activas del motor antifraude,
 * leídas desde la tabla regla_fraude.
 */
@RestController
@RequestMapping("/api/v1/rule")
public class RuleApiController {

    private final ReglaConsultaService reglaService;
    private final ReglaResponseMapper mapper;

    public RuleApiController(ReglaConsultaService reglaService, ReglaResponseMapper mapper) {
        this.reglaService = reglaService;
        this.mapper = mapper;
    }

    /** Devuelve la lista de reglas activas almacenadas en la base de datos. */
    @GetMapping
    public ResponseEntity<List<ReglaResponse>> list() {
        return ResponseEntity.ok(reglaService.listar().stream().map(mapper::toResponse).toList());
    }
}

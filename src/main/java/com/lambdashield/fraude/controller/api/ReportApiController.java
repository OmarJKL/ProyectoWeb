package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.model.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller — CAPA CONTROLADOR
 *
 * Recurso: /api/v1/report
 * Responsabilidad única: generar y exportar reportes del sistema.
 *
 * Separado del ReporteController (vista HTML) para mantener la
 * separación entre endpoints REST y controllers de páginas.
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportApiController {

    private final ReporteService reporteService;

    public ReportApiController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Exporta el historial completo de evaluaciones en formato CSV. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = reporteService.exportarTransaccionesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte_transacciones_lambda.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}

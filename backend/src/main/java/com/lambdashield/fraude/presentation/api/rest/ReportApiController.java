package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.presentation.export.ReporteCsvExporter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para /api/v1/report: genera y exporta reportes del sistema.
 * Separado del ReporteController (vista HTML) para distinguir endpoints REST de páginas.
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportApiController {

    private final ReporteCsvExporter reporteCsvExporter;

    public ReportApiController(ReporteCsvExporter reporteCsvExporter) {
        this.reporteCsvExporter = reporteCsvExporter;
    }

    /** Exporta el historial completo de evaluaciones en formato CSV. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = reporteCsvExporter.exportarTransacciones();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte_transacciones_lambda.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}

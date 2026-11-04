package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.presentation.export.ReporteCsvExporter;
import com.lambdashield.fraude.presentation.export.ReporteExcelExporter;
import com.lambdashield.fraude.presentation.export.ReportePdfExporter;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller para /api/v1/report: exporta el historial de evaluaciones en
 * CSV, Excel o PDF, con filtros opcionales por fecha de evaluación, nivel de
 * riesgo y cliente.
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportApiController {

    private final EvaluacionConsultaService evaluacionConsultaService;
    private final EvaluacionResponseMapper mapper;
    private final ReporteCsvExporter csvExporter;
    private final ReporteExcelExporter excelExporter;
    private final ReportePdfExporter pdfExporter;

    public ReportApiController(EvaluacionConsultaService evaluacionConsultaService,
                               EvaluacionResponseMapper mapper,
                               ReporteCsvExporter csvExporter,
                               ReporteExcelExporter excelExporter,
                               ReportePdfExporter pdfExporter) {
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.mapper = mapper;
        this.csvExporter = csvExporter;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
    }

    /** Exporta el historial filtrado en el formato indicado (csv, xlsx o pdf). */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv")
            @Pattern(regexp = "csv|xlsx|pdf", message = "Formato inválido (permitidos: csv, xlsx, pdf)")
            String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false)
            @Pattern(regexp = "bajo|medio|alto|critico", message = "Nivel inválido (permitidos: bajo, medio, alto, critico)")
            String nivel,
            @RequestParam(required = false) String cliente) {

        List<EvaluacionResponse> filas = mapper.toResponses(
                evaluacionConsultaService.listarFiltrado(desde, hasta, nivel, cliente));

        return switch (formato) {
            case "xlsx" -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_transacciones_lambda.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelExporter.exportar(filas));
            case "pdf" -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_transacciones_lambda.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfExporter.exportar(filas));
            default -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_transacciones_lambda.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvExporter.exportar(filas));
        };
    }
}

package com.lambdashield.fraude.controller.mvc;

import com.lambdashield.fraude.service.ReporteService;
import com.lambdashield.fraude.service.TransaccionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReporteController {

    private final ReporteService reporteService;
    private final TransaccionService transaccionService;

    public ReporteController(ReporteService reporteService, TransaccionService transaccionService) {
        this.reporteService = reporteService;
        this.transaccionService = transaccionService;
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model) {
        model.addAttribute("resumenReporte", reporteService.obtenerResumen());
        model.addAttribute("transacciones", transaccionService.listarTodas());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        model.addAttribute("rol", session.getAttribute("rol"));
        return "reportes";
    }

    @GetMapping("/api/v1/reportes/transacciones/csv")
    public ResponseEntity<byte[]> descargarCsv() {
        byte[] csvData = reporteService.exportarTransaccionesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_transacciones_lambda.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}

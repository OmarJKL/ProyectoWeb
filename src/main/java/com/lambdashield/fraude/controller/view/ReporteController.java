package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.service.ReporteService;
import com.lambdashield.fraude.model.service.TransaccionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC Controller — CAPA CONTROLADOR
 *
 * Responsabilidad única: renderizar la vista HTML de reportes.
 * La exportación CSV se maneja en ReportApiController (/api/v1/report/export).
 */
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
}

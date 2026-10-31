package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.application.service.ReporteResumenService;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador MVC que renderiza la vista HTML de reportes.
 * La exportación CSV se maneja en ReportApiController (/api/v1/report/export).
 */
@Controller
public class ReporteController {

    private final ReporteResumenService reporteService;
    private final EvaluacionConsultaService evaluacionConsultaService;
    private final EvaluacionResponseMapper mapper;

    public ReporteController(ReporteResumenService reporteService,
                             EvaluacionConsultaService evaluacionConsultaService,
                             EvaluacionResponseMapper mapper) {
        this.reporteService = reporteService;
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.mapper = mapper;
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model) {
        model.addAttribute("resumenReporte", reporteService.obtenerResumen());
        model.addAttribute("transacciones", mapper.toResponses(evaluacionConsultaService.listarTodas()));
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        model.addAttribute("rol", session.getAttribute("rol"));
        return "reportes";
    }
}

package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.application.service.ReporteResumenService;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controlador MVC que renderiza la vista HTML de reportes, con los mismos
 * filtros que la exportación (fecha de evaluación, nivel y cliente).
 * La exportación CSV/Excel/PDF se maneja en ReportApiController.
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
    public String reportes(HttpSession session, Model model,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(required = false) String nivel,
                           @RequestParam(required = false) String cliente) {
        var filas = evaluacionConsultaService.listarFiltrado(desde, hasta, nivel, cliente);

        model.addAttribute("resumenReporte", reporteService.resumir(filas));
        model.addAttribute("transacciones", mapper.toResponses(filas));
        model.addAttribute("filtros", new ReporteController.FiltroReporte(desde, hasta, nivel, cliente));
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        model.addAttribute("rol", session.getAttribute("rol"));
        return "reportes";
    }

    /** Estado de los filtros del formulario para conservarlos en la vista. */
    public record FiltroReporte(LocalDate desde, LocalDate hasta, String nivel, String cliente) {
        public boolean activos() {
            return desde != null || hasta != null
                    || (nivel != null && !nivel.isBlank())
                    || (cliente != null && !cliente.isBlank());
        }
    }
}

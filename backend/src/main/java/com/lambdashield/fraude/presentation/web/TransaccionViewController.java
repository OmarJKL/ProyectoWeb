package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.application.service.EvaluacionMetricasService;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransaccionViewController {

    private final EvaluacionConsultaService evaluacionConsultaService;
    private final EvaluacionMetricasService evaluacionMetricasService;
    private final EvaluacionResponseMapper mapper;

    public TransaccionViewController(EvaluacionConsultaService evaluacionConsultaService,
                                     EvaluacionMetricasService evaluacionMetricasService,
                                     EvaluacionResponseMapper mapper) {
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.evaluacionMetricasService = evaluacionMetricasService;
        this.mapper = mapper;
    }

    @GetMapping("/transacciones")
    public String transacciones(HttpSession session, Model model) {
        model.addAttribute("transacciones", mapper.toResponses(evaluacionConsultaService.listarTodas()));
        model.addAttribute("kpis", mapper.toResponse(evaluacionMetricasService.calcularKpis()));
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "transacciones";
    }
}

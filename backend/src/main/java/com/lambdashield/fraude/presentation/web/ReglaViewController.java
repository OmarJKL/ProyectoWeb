package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.ReglaConsultaService;
import com.lambdashield.fraude.presentation.mapper.ReglaResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador MVC que renderiza la vista HTML de reglas del motor.
 * Los datos se obtienen de la tabla regla_fraude.
 */
@Controller
public class ReglaViewController {

    private final ReglaConsultaService reglaService;
    private final ReglaResponseMapper mapper;

    public ReglaViewController(ReglaConsultaService reglaService, ReglaResponseMapper mapper) {
        this.reglaService = reglaService;
        this.mapper = mapper;
    }

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        model.addAttribute("reglas", reglaService.listar().stream().map(mapper::toResponse).toList());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "reglas";
    }
}

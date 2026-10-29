package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.service.ReglaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC Controller - CAPA PRESENTACIÓN / WEB
 *
 * Responsabilidad única: renderizar la vista HTML de reglas del motor.
 * Los datos se obtienen de la tabla regla_fraude.
 */
@Controller
public class ReglaViewController {

    private final ReglaService reglaService;

    public ReglaViewController(ReglaService reglaService) {
        this.reglaService = reglaService;
    }

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        model.addAttribute("reglas", reglaService.listar());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "reglas";
    }
}

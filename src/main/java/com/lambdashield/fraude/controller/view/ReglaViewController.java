package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.controller.api.RuleApiController;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC Controller — CAPA CONTROLADOR
 *
 * Responsabilidad única: renderizar la vista HTML de reglas del motor.
 * Los datos de reglas provienen de RuleApiController (fuente de verdad).
 */
@Controller
public class ReglaViewController {

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        model.addAttribute("reglas", RuleApiController.RULES);
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "reglas";
    }
}

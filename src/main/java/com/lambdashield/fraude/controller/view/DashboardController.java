package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("resumen", dashboardService.obtenerResumen());
        model.addAttribute("movimientosRecientes", dashboardService.obtenerMovimientosRecientes(8));
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "dashboard";
    }
}

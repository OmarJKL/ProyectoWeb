package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.DashboardMovimientosService;
import com.lambdashield.fraude.application.service.DashboardResumenService;
import com.lambdashield.fraude.presentation.mapper.DashboardResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardResumenService dashboardResumenService;
    private final DashboardMovimientosService dashboardMovimientosService;
    private final DashboardResponseMapper mapper;

    public DashboardController(DashboardResumenService dashboardResumenService,
                               DashboardMovimientosService dashboardMovimientosService,
                               DashboardResponseMapper mapper) {
        this.dashboardResumenService = dashboardResumenService;
        this.dashboardMovimientosService = dashboardMovimientosService;
        this.mapper = mapper;
    }

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("resumen", mapper.toResponse(dashboardResumenService.obtenerResumen()));
        model.addAttribute("movimientosRecientes", dashboardMovimientosService.obtenerMovimientosRecientes(8)
                .stream().map(mapper::toResponse).toList());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "dashboard";
    }
}

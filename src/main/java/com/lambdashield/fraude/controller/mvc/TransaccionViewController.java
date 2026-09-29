package com.lambdashield.fraude.controller.mvc;

import com.lambdashield.fraude.service.TransaccionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransaccionViewController {

    private final TransaccionService transaccionService;

    public TransaccionViewController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @GetMapping("/transacciones")
    public String transacciones(HttpSession session, Model model) {
        model.addAttribute("transacciones", transaccionService.listarTodas());
        model.addAttribute("kpis", transaccionService.calcularKpis());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "transacciones";
    }
}

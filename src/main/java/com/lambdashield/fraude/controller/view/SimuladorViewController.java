package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SimuladorViewController {

    private final ClienteService clienteService;

    public SimuladorViewController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/simulador")
    public String simulador(HttpSession session, Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "simulador";
    }
}

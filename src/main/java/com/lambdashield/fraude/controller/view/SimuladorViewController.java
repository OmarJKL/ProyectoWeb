package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.store.ClienteStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SimuladorViewController {

    private final ClienteStore clienteStore;

    public SimuladorViewController(ClienteStore clienteStore) {
        this.clienteStore = clienteStore;
    }

    @GetMapping("/simulador")
    public String simulador(HttpSession session, Model model) {
        model.addAttribute("clientes", clienteStore.listarTodos());
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "simulador";
    }
}

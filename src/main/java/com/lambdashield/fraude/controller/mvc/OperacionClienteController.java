package com.lambdashield.fraude.controller.mvc;

import com.lambdashield.fraude.service.TransaccionService;
import com.lambdashield.fraude.store.ClienteStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class OperacionClienteController {

    private final ClienteStore clienteStore;
    private final TransaccionService transaccionService;

    public OperacionClienteController(ClienteStore clienteStore, TransaccionService transaccionService) {
        this.clienteStore = clienteStore;
        this.transaccionService = transaccionService;
    }

    @GetMapping("/operacion")
    public String operacion(HttpSession session, Model model) {
        String usuario = (String) session.getAttribute("usuario");
        String nombreUsuario = (String) session.getAttribute("nombreUsuario");

        // Datos del cliente en sesión (por defecto María Fernández C-001)
        ClienteStore.ClienteInfo cliente = clienteStore.buscar("C-001");

        model.addAttribute("cliente", cliente);
        model.addAttribute("nombreUsuario", nombreUsuario != null ? nombreUsuario : "Cliente");
        model.addAttribute("rol", session.getAttribute("rol"));
        model.addAttribute("saldo", 4850.00); // Saldo simulado del cliente
        List<com.lambdashield.fraude.dto.response.EvaluacionResponse> misTx = transaccionService.listarPorCliente("María");
        if (misTx.isEmpty()) {
            misTx = transaccionService.listarTodas();
        }
        model.addAttribute("transacciones", misTx);

        return "cliente/operacion";
    }
}

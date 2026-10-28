package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.service.TransaccionService;
import com.lambdashield.fraude.model.store.ClienteStore;
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
        // El cliente se toma siempre de la sesión autenticada, nunca de un valor fijo.
        String codigoCliente = (String) session.getAttribute("codigoCliente");
        String nombreUsuario = (String) session.getAttribute("nombreUsuario");

        ClienteStore.ClienteInfo cliente = codigoCliente != null ? clienteStore.buscar(codigoCliente) : null;

        model.addAttribute("cliente", cliente);
        model.addAttribute("codigoCliente", codigoCliente);
        model.addAttribute("nombreUsuario", nombreUsuario != null ? nombreUsuario : "Cliente");
        model.addAttribute("rol", session.getAttribute("rol"));
        model.addAttribute("saldo", 4850.00); // Saldo simulado del cliente

        // Solo las operaciones del cliente autenticado. Sin resultados: lista vacía (nunca las de otros).
        List<EvaluacionResponse> misTx = codigoCliente != null
                ? transaccionService.listarPorClienteCodigo(codigoCliente)
                : List.of();
        model.addAttribute("transacciones", misTx);

        return "cliente/operacion";
    }
}

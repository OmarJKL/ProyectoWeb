package com.lambdashield.fraude.controller.view;

import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.service.ClienteService;
import com.lambdashield.fraude.model.service.TransaccionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class OperacionClienteController {

    private final ClienteService clienteService;
    private final TransaccionService transaccionService;

    public OperacionClienteController(ClienteService clienteService, TransaccionService transaccionService) {
        this.clienteService = clienteService;
        this.transaccionService = transaccionService;
    }

    @GetMapping("/operacion")
    public String operacion(HttpSession session, Model model) {
        // El cliente se toma siempre de la sesión autenticada, nunca de un valor fijo.
        String codigoCliente = (String) session.getAttribute("codigoCliente");
        String nombreUsuario = (String) session.getAttribute("nombreUsuario");

        ClienteService.ClienteInfo cliente = codigoCliente != null
                ? clienteService.buscar(codigoCliente).orElse(null)
                : null;

        model.addAttribute("cliente", cliente);
        model.addAttribute("codigoCliente", codigoCliente);
        model.addAttribute("nombreUsuario", nombreUsuario != null ? nombreUsuario : "Cliente");
        model.addAttribute("rol", session.getAttribute("rol"));
        model.addAttribute("numeroCuenta", cliente != null ? cliente.numeroCuenta() : "");
        model.addAttribute("saldo", cliente != null ? cliente.saldo() : 0.0);

        // Solo las operaciones del cliente autenticado. Sin resultados: lista vacía (nunca las de otros).
        List<EvaluacionResponse> misTx = codigoCliente != null
                ? transaccionService.listarPorClienteCodigo(codigoCliente)
                : List.of();
        model.addAttribute("transacciones", misTx);

        return "cliente/operacion";
    }
}

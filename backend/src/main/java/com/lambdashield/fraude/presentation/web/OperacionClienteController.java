package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.application.service.ClienteService;
import com.lambdashield.fraude.application.service.EvaluacionConsultaService;
import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class OperacionClienteController {

    private final ClienteService clienteService;
    private final EvaluacionConsultaService evaluacionConsultaService;
    private final EvaluacionResponseMapper mapper;

    public OperacionClienteController(ClienteService clienteService,
                                      EvaluacionConsultaService evaluacionConsultaService,
                                      EvaluacionResponseMapper mapper) {
        this.clienteService = clienteService;
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.mapper = mapper;
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

        // Solo las operaciones del cliente autenticado. Sin cliente: lista vacía, nunca las de otros.
        List<EvaluacionResponse> misTx = codigoCliente != null
                ? mapper.toResponses(evaluacionConsultaService.listarPorClienteCodigo(codigoCliente))
                : List.of();
        model.addAttribute("transacciones", misTx);

        return "cliente/operacion";
    }
}

package com.lambdashield.fraude.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renderiza la página de historial. Los datos y los KPI se obtienen desde el
 * navegador llamando a /api/v1/evaluation y /api/v1/metric, por lo que la
 * plantilla no necesita modelo precargado.
 */
@Controller
public class TransaccionViewController {

    @GetMapping("/transacciones")
    public String transacciones() {
        return "transacciones";
    }
}

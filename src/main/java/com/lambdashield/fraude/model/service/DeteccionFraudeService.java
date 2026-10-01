package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import com.lambdashield.fraude.model.rule.ReglaEvaluable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeteccionFraudeService {
    private final List<ReglaEvaluable> reglas;

    public DeteccionFraudeService(List<ReglaEvaluable> reglas) {
        this.reglas = reglas;
    }

    public List<FactorResponse> evaluar(TransaccionCrearRequest request, double promedioCliente) {
        List<FactorResponse> factores = new ArrayList<>();
        for (ReglaEvaluable regla : reglas) {
            factores.add(regla.evaluar(request, promedioCliente));
        }
        return factores;
    }

    public int calcularPuntajeTotal(List<FactorResponse> factores) {
        int suma = factores.stream().mapToInt(FactorResponse::puntaje).sum();
        return Math.min(100, suma);
    }

    public String clasificarNivel(int puntaje) {
        if (puntaje < 30) return "bajo";
        if (puntaje < 60) return "medio";
        if (puntaje < 80) return "alto";
        return "critico";
    }

    public String determinarEstado(String nivel) {
        return switch (nivel) {
            case "bajo" -> "Aprobada";
            case "medio" -> "Monitoreo";
            case "alto" -> "Revisión manual";
            default -> "Bloqueada";
        };
    }

    public String obtenerIconoEstado(String nivel) {
        return switch (nivel) {
            case "bajo" -> "bi-check-circle-fill";
            case "medio" -> "bi-eye-fill";
            case "alto" -> "bi-flag-fill";
            default -> "bi-x-octagon-fill";
        };
    }

    public String obtenerColorNivel(String nivel) {
        return switch (nivel) {
            case "bajo" -> "var(--riesgo-bajo)";
            case "medio" -> "var(--riesgo-medio)";
            case "alto" -> "var(--riesgo-alto)";
            default -> "var(--riesgo-critico)";
        };
    }
}

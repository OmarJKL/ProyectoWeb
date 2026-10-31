package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.fraud.FactorEvaluado;
import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.domain.fraud.rule.ReglaEvaluable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeteccionFraudeService {
    private final List<ReglaEvaluable> reglas;

    public DeteccionFraudeService(List<ReglaEvaluable> reglas) {
        this.reglas = reglas;
    }

    public List<FactorEvaluado> evaluar(TransaccionEvaluable transaccion, double promedioCliente) {
        List<FactorEvaluado> factores = new ArrayList<>();
        for (ReglaEvaluable regla : reglas) {
            factores.add(regla.evaluar(transaccion, promedioCliente));
        }
        return factores;
    }

    public int calcularPuntajeTotal(List<FactorEvaluado> factores) {
        int suma = factores.stream().mapToInt(FactorEvaluado::puntaje).sum();
        return Math.min(100, suma);
    }

    public String clasificarNivel(int puntaje) {
        if (puntaje < 30) return "bajo";
        if (puntaje < 60) return "medio";
        if (puntaje < 80) return "alto";
        return "critico";
    }

}

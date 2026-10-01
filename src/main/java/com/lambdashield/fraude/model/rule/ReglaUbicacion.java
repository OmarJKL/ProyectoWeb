package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaUbicacion implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 30; // RF004: Peso inicial de 30 según punto 18 del PDF
        String pais = tr.pais() != null ? tr.pais() : "local";
        int puntaje;
        String motivo;

        switch (pais) {
            case "regional" -> {
                puntaje = 15;
                motivo = "Operación desde país regional vecino no habitual (es_pais_nuevo = true).";
            }
            case "alto_riesgo" -> {
                puntaje = max;
                motivo = "Operación desde jurisdicción internacional de alto riesgo (es_pais_nuevo = true).";
            }
            default -> {
                puntaje = 0;
                motivo = "Operación ejecutada en el país habitual del titular.";
            }
        }
        return new FactorResponse("ubicacion", "RF004 · País nuevo", puntaje, max, motivo);
    }
}

package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaDispositivo implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 15; // RF002: Peso inicial de 15 según punto 18 del PDF
        if ("nuevo".equalsIgnoreCase(tr.dispositivo())) {
            return new FactorResponse("dispositivo", "RF002 · Dispositivo nuevo", max, max, "Dispositivo no reconocido sin historial previo (es_dispositivo_nuevo = true).");
        }
        return new FactorResponse("dispositivo", "RF002 · Dispositivo nuevo", 0, max, "Dispositivo habitual previamente reconocido por el cliente.");
    }
}

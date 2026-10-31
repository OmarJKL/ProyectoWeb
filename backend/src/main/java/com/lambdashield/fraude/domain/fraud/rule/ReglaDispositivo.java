package com.lambdashield.fraude.domain.fraud.rule;

import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.domain.fraud.FactorEvaluado;

public class ReglaDispositivo implements ReglaEvaluable {
    @Override
    public FactorEvaluado evaluar(TransaccionEvaluable tr, double promedioCliente) {
        int max = 15; // RF002: Peso inicial de 15 según punto 18 del PDF
        if ("nuevo".equalsIgnoreCase(tr.dispositivo())) {
            return new FactorEvaluado("dispositivo", "RF002 – Dispositivo nuevo", max, max, "Dispositivo no reconocido sin historial previo (es_dispositivo_nuevo = true).");
        }
        return new FactorEvaluado("dispositivo", "RF002 – Dispositivo nuevo", 0, max, "Dispositivo habitual previamente reconocido por el cliente.");
    }
}

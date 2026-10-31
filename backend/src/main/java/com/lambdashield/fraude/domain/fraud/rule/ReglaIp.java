package com.lambdashield.fraude.domain.fraud.rule;

import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.domain.fraud.FactorEvaluado;

public class ReglaIp implements ReglaEvaluable {
    @Override
    public FactorEvaluado evaluar(TransaccionEvaluable tr, double promedioCliente) {
        int max = 10; // RF003: Peso inicial de 10 según punto 18 del PDF
        if ("nueva".equalsIgnoreCase(tr.ip())) {
            return new FactorEvaluado("ip", "RF003 – IP nueva", max, max, "Conexión desde una dirección IP no registrada previamente (es_ip_nueva = true).");
        }
        return new FactorEvaluado("ip", "RF003 – IP nueva", 0, max, "Conexión desde una dirección IP habitual e histórica.");
    }
}

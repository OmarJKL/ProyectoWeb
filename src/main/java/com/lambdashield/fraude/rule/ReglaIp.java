package com.lambdashield.fraude.rule;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaIp implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 10; // RF003: Peso inicial de 10 según punto 18 del PDF
        if ("nueva".equalsIgnoreCase(tr.ip())) {
            return new FactorResponse("ip", "RF003 · IP nueva", max, max, "Conexión desde una dirección IP no registrada previamente (es_ip_nueva = true).");
        }
        return new FactorResponse("ip", "RF003 · IP nueva", 0, max, "Conexión desde una dirección IP habitual e histórica.");
    }
}

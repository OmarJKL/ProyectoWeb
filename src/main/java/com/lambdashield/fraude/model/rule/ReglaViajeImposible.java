package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaViajeImposible implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 40; // RF007: Peso inicial de 40 según punto 18 del PDF
        String pais = tr.pais() != null ? tr.pais() : "local";
        int operaciones = tr.operacionesUltimaHora();
        boolean ipNueva = "nueva".equalsIgnoreCase(tr.ip());

        if (("alto_riesgo".equals(pais) || "regional".equals(pais)) && operaciones >= 2) {
            return new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", max, max,
                    "Distancia geográfica vs tiempo excede umbral físico: operaciones registradas en <1h desde ubicaciones incompatibles.");
        } else if ("alto_riesgo".equals(pais) && ipNueva) {
            return new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", 28, max,
                    "Conexión remota inmediata desde IP no habitual en jurisdicción de alto riesgo (posible proxy/VPN o viaje imposible).");
        } else if ("casino".equalsIgnoreCase(tr.categoria()) || "cripto".equalsIgnoreCase(tr.categoria())) {
            return new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", 15, max,
                    "Operación en comercio de liquidación inmediata transfronteriza sin validación de presencia física.");
        }

        return new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", 0, max,
                "Velocidad de desplazamiento y origen geográfico dentro de los límites físicos plausibles.");
    }
}
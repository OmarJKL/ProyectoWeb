package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaVelocidad implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 25; // RF006: Peso inicial de 25 según punto 18 del PDF
        int n = tr.operacionesUltimaHora();
        double puntaje;
        String motivo;

        if (n <= 1) {
            puntaje = 0;
            motivo = "Frecuencia normal: 1 operación en la última hora.";
        } else if (n <= 3) {
            puntaje = EscalarUtil.escalar(n, 2.0, 3.0, 5.0, 12.0);
            motivo = n + " operaciones en la última hora: frecuencia moderadamente elevada.";
        } else if (n <= 5) {
            puntaje = EscalarUtil.escalar(n, 4.0, 5.0, 15.0, 20.0);
            motivo = n + " operaciones en la última hora: alerta de tráfico acelerado.";
        } else {
            puntaje = max;
            motivo = n + " operaciones en la última hora (> 5 transacciones/1h): patrón crítico de ráfaga/card testing.";
        }
        return new FactorResponse("velocidad", "RF006 · Alta frecuencia", (int) Math.round(puntaje), max, motivo);
    }
}

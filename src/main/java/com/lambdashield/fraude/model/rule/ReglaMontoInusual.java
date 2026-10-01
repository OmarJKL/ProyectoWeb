package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ReglaMontoInusual implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 25; // RF001: Peso inicial de 25 según punto 18 del PDF
        double promedio = promedioCliente > 0 ? promedioCliente : 100.0;
        double proporcion = tr.monto() / promedio;

        double puntaje;
        String motivo;

        if (proporcion <= 1.5) {
            puntaje = 0;
            motivo = String.format(Locale.US, "Monto S/ %.2f dentro del rango habitual (prom. S/ %.2f).", tr.monto(), promedio);
        } else if (proporcion <= 3.0) {
            puntaje = EscalarUtil.escalar(proporcion, 1.5, 3.0, 5.0, 15.0);
            motivo = String.format(Locale.US, "Monto %.1fx por encima del promedio del titular.", proporcion);
        } else if (proporcion < 5.0) {
            puntaje = EscalarUtil.escalar(proporcion, 3.0, 5.0, 15.0, 22.0);
            motivo = String.format(Locale.US, "Monto %.1fx por encima del promedio habitual.", proporcion);
        } else {
            puntaje = max;
            motivo = String.format(Locale.US, "Monto %.1fx excede el umbral crítico (>5x promedio_30d).", proporcion);
        }

        return new FactorResponse("monto", "RF001 · Monto inusual", (int) Math.round(puntaje), max, motivo);
    }
}

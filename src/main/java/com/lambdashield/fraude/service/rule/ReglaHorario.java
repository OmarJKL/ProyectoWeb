package com.lambdashield.fraude.service.rule;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.FactorResponse;
import org.springframework.stereotype.Component;

@Component
public class ReglaHorario implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 10; // RF005: Peso inicial de 10 según punto 18 del PDF
        int hora = tr.hora();
        int puntaje;
        String motivo;

        if (hora >= 0 && hora < 5) {
            puntaje = max;
            motivo = String.format("Operación a las %02d:00 en franja de madrugada atípica (es_horario_inusual = true).", hora);
        } else {
            puntaje = 0;
            motivo = "Operación fuera de la franja de madrugada (00:00–04:59); no genera puntaje.";
        }
        return new FactorResponse("horario", "RF005 – Horario inusual", puntaje, max, motivo);
    }
}

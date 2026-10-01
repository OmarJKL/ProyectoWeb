package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
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
        } else if (hora >= 22) {
            puntaje = 4;
            motivo = String.format("Operación nocturna (%02d:00), fuera de horario regular.", hora);
        } else {
            puntaje = 0;
            motivo = "Operación dentro del horario habitual de actividad.";
        }
        return new FactorResponse("horario", "RF005 · Horario inusual", puntaje, max, motivo);
    }
}

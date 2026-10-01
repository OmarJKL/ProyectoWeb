package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ReglaBeneficiario implements ReglaEvaluable {
    @Override
    public FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente) {
        int max = 20; // RF008: Peso inicial de 20 según punto 18 del PDF
        double umbral = promedioCliente > 0 ? promedioCliente * 1.5 : 500.0;
        boolean esNuevo = "nuevo".equalsIgnoreCase(tr.beneficiario());

        if (esNuevo && tr.monto() > umbral) {
            return new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", max, max,
                    String.format(Locale.US, "Nuevo destinatario con monto elevado (S/ %.2f > umbral S/ %.2f).", tr.monto(), umbral));
        } else if (esNuevo) {
            return new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", 10, max,
                    "Nuevo destinatario, pero monto dentro del rango operativo moderado.");
        }
        return new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", 0, max,
                "Destinatario recurrente previamente validado.");
    }
}

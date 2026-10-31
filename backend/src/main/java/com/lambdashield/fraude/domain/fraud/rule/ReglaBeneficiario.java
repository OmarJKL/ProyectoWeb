package com.lambdashield.fraude.domain.fraud.rule;

import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.domain.fraud.FactorEvaluado;

import java.util.Locale;

public class ReglaBeneficiario implements ReglaEvaluable {
    @Override
    public FactorEvaluado evaluar(TransaccionEvaluable tr, double promedioCliente) {
        int max = 20; // RF008: Peso inicial de 20 según punto 18 del PDF
        double umbral = promedioCliente > 0 ? promedioCliente * 1.5 : 500.0;
        boolean esNuevo = "nuevo".equalsIgnoreCase(tr.beneficiario());

        if (esNuevo && tr.monto() > umbral) {
            return new FactorEvaluado("beneficiario", "RF008 – Beneficiario nuevo + monto alto", max, max,
                    String.format(Locale.US, "Nuevo destinatario con monto elevado (S/ %.2f > umbral S/ %.2f).", tr.monto(), umbral));
        }
        if (esNuevo) {
            return new FactorEvaluado("beneficiario", "RF008 – Beneficiario nuevo + monto alto", 0, max,
                    "Nuevo destinatario, pero monto dentro del rango operativo moderado (sin puntaje).");
        }
        return new FactorEvaluado("beneficiario", "RF008 – Beneficiario nuevo + monto alto", 0, max,
                "Destinatario recurrente previamente validado.");
    }
}

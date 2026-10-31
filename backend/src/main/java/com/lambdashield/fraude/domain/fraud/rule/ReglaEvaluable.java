package com.lambdashield.fraude.domain.fraud.rule;

import com.lambdashield.fraude.domain.fraud.FactorEvaluado;
import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;

/** Contrato extensible de una regla del motor antifraude. */
public interface ReglaEvaluable {
    FactorEvaluado evaluar(TransaccionEvaluable transaccion, double promedioCliente);
}

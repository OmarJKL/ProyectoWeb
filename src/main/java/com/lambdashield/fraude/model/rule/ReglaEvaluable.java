package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.FactorResponse;

public interface ReglaEvaluable {
    FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente);
}

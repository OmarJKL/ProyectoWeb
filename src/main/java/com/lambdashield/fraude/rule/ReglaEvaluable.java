package com.lambdashield.fraude.rule;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.FactorResponse;

public interface ReglaEvaluable {
    FactorResponse evaluar(TransaccionCrearRequest tr, double promedioCliente);
}

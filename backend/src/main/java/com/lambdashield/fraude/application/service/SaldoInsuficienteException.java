package com.lambdashield.fraude.application.service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Se lanza cuando una operación aprobada excede el saldo disponible de la
 * cuenta del cliente.
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String numeroCuenta, BigDecimal saldo, BigDecimal monto) {
        super(String.format(Locale.ROOT,
                "Saldo insuficiente: la cuenta %s tiene S/ %.2f disponible y la operación es de S/ %.2f",
                numeroCuenta, saldo, monto));
    }
}

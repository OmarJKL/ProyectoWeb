package com.lambdashield.fraude.domain.customer;

import com.lambdashield.fraude.domain.entity.Cliente;

/** Formatea el nombre visible de un cliente de manera consistente. */
public final class NombreClienteFormatter {

    private NombreClienteFormatter() {}

    public static String nombreCompleto(Cliente cliente) {
        StringBuilder nombre = new StringBuilder();
        agregar(nombre, cliente.getNombres());
        agregar(nombre, cliente.getApellidoPaterno());
        agregar(nombre, cliente.getApellidoMaterno());
        return nombre.isEmpty() ? cliente.getCodigoCliente() : nombre.toString();
    }

    private static void agregar(StringBuilder nombre, String parte) {
        if (parte == null || parte.isBlank()) return;
        if (!nombre.isEmpty()) nombre.append(' ');
        nombre.append(parte.trim());
    }
}

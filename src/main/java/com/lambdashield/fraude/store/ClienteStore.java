package com.lambdashield.fraude.store;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClienteStore {

    public record ClienteInfo(String codigo, String nombre, double promedioGasto) {}

    private final Map<String, ClienteInfo> clientes = new ConcurrentHashMap<>();

    public ClienteStore() {
        clientes.put("C-001", new ClienteInfo("C-001", "María Fernández", 180.0));
        clientes.put("C-002", new ClienteInfo("C-002", "Jorge Salas", 950.0));
        clientes.put("C-003", new ClienteInfo("C-003", "Ana Quiroz", 60.0));
    }

    public ClienteInfo buscar(String codigo) {
        return clientes.get(codigo);
    }

    public List<ClienteInfo> listarTodos() {
        return new ArrayList<>(clientes.values());
    }
}

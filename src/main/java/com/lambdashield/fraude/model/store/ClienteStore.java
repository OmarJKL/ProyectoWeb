package com.lambdashield.fraude.model.store;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClienteStore {

    /**
     * @param dispositivosConocidos huellas normalizadas "Navegador|SistemaOperativo"
     * @param ipsConocidas          direcciones IP desde las que opera habitualmente el cliente
     */
    public record ClienteInfo(String codigo, String nombre, double promedioGasto,
                              Set<String> dispositivosConocidos, Set<String> ipsConocidas) {}

    private static final Set<String> IPS_LOCALES = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1");

    private final Map<String, ClienteInfo> clientes = new ConcurrentHashMap<>();

    public ClienteStore() {
        clientes.put("C-001", new ClienteInfo("C-001", "María Fernández", 180.0,
                Set.of("Chrome|Windows", "Edge|Windows"), IPS_LOCALES));
        clientes.put("C-002", new ClienteInfo("C-002", "Jorge Salas", 950.0,
                Set.of("Chrome|Android", "Chrome|Windows"), IPS_LOCALES));
        clientes.put("C-003", new ClienteInfo("C-003", "Ana Quiroz", 60.0,
                Set.of("Firefox|Windows", "Safari|macOS"), IPS_LOCALES));
    }

    public ClienteInfo buscar(String codigo) {
        return codigo == null ? null : clientes.get(codigo);
    }

    public List<ClienteInfo> listarTodos() {
        return new ArrayList<>(clientes.values());
    }

    /** Indica si el User-Agent corresponde a un dispositivo ya conocido del cliente. */
    public boolean esDispositivoConocido(String codigoCliente, String userAgent) {
        ClienteInfo info = buscar(codigoCliente);
        return info != null && info.dispositivosConocidos().contains(normalizarDispositivo(userAgent));
    }

    /** Indica si la IP remota pertenece al conjunto de IPs habituales del cliente. */
    public boolean esIpConocida(String codigoCliente, String ip) {
        ClienteInfo info = buscar(codigoCliente);
        return info != null && ip != null && info.ipsConocidas().contains(ip);
    }

    /**
     * Reduce el User-Agent a una huella estable "Navegador|SistemaOperativo",
     * ignorando la versión para no invalidar el dispositivo con cada actualización.
     */
    private String normalizarDispositivo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "desconocido";
        }

        String navegador;
        if (userAgent.contains("Edg/")) {
            navegador = "Edge";
        } else if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            navegador = "Opera";
        } else if (userAgent.contains("Firefox/")) {
            navegador = "Firefox";
        } else if (userAgent.contains("Chrome/")) {
            navegador = "Chrome";
        } else if (userAgent.contains("Safari/")) {
            navegador = "Safari";
        } else {
            navegador = "Otro";
        }

        String sistema;
        if (userAgent.contains("Windows")) {
            sistema = "Windows";
        } else if (userAgent.contains("Android")) {
            sistema = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            sistema = "iOS";
        } else if (userAgent.contains("Mac OS X")) {
            sistema = "macOS";
        } else if (userAgent.contains("Linux")) {
            sistema = "Linux";
        } else {
            sistema = "Otro";
        }

        return navegador + "|" + sistema;
    }
}


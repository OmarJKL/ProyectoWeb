package com.lambdashield.fraude.store;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UsuarioStore {

    public record UsuarioInfo(String usuario, String clave, String nombreCompleto, String rol) {}

    private final Map<String, UsuarioInfo> usuarios = new ConcurrentHashMap<>();

    public UsuarioStore() {
        // Administrador con acceso completo a monitoreo, reglas, gráficas y reportes
        usuarios.put("admin", new UsuarioInfo("admin", "lambda2026", "Administrador del Sistema", "ADMIN"));
        usuarios.put("analista", new UsuarioInfo("analista", "fraude123", "Analista Antifraude", "ADMIN"));

        // Usuario estándar (cliente de banca digital que realiza transacciones)
        usuarios.put("usuario", new UsuarioInfo("usuario", "cliente123", "María Fernández (Cliente)", "CLIENTE"));
        usuarios.put("maria", new UsuarioInfo("maria", "123456", "María Fernández (Cliente)", "CLIENTE"));
        usuarios.put("cliente", new UsuarioInfo("cliente", "cliente123", "María Fernández (Cliente)", "CLIENTE"));
    }

    public Optional<UsuarioInfo> validar(String usuario, String clave) {
        UsuarioInfo info = usuarios.get(usuario);
        if (info != null && info.clave().equals(clave)) {
            return Optional.of(info);
        }
        return Optional.empty();
    }

    public Optional<UsuarioInfo> buscarPorUsuario(String usuario) {
        return Optional.ofNullable(usuarios.get(usuario));
    }
}

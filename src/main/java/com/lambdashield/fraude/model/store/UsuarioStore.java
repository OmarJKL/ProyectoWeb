package com.lambdashield.fraude.model.store;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UsuarioStore {

    public record UsuarioInfo(String usuario, String clave, String nombreCompleto, String rol) {}

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, UsuarioInfo> usuarios = new ConcurrentHashMap<>();

    public UsuarioStore() {
        usuarios.put("admin", new UsuarioInfo("admin", passwordEncoder.encode("lambda2026"), "Administrador del Sistema", "ADMIN"));
        usuarios.put("analista", new UsuarioInfo("analista", passwordEncoder.encode("fraude123"), "Analista Antifraude", "ANALISTA"));

        usuarios.put("usuario", new UsuarioInfo("usuario", passwordEncoder.encode("cliente123"), "María Fernández (Cliente)", "CLIENTE"));
        usuarios.put("maria", new UsuarioInfo("maria", passwordEncoder.encode("123456"), "María Fernández (Cliente)", "CLIENTE"));
        usuarios.put("cliente", new UsuarioInfo("cliente", passwordEncoder.encode("cliente123"), "María Fernández (Cliente)", "CLIENTE"));
    }

    public Optional<UsuarioInfo> validar(String usuario, String clave) {
        UsuarioInfo info = usuarios.get(usuario);
        if (info != null && passwordEncoder.matches(clave, info.clave())) {
            return Optional.of(info);
        }
        return Optional.empty();
    }

    public Optional<UsuarioInfo> buscarPorUsuario(String usuario) {
        return Optional.ofNullable(usuarios.get(usuario));
    }
}

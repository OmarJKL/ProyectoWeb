package com.lambdashield.fraude.model.store;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UsuarioStore {

    /**
     * @param clave hash BCrypt de la contraseña (nunca la clave en texto plano)
     * @param codigoCliente código del cliente asociado (solo rol CLIENTE)
     */
    public record UsuarioInfo(String usuario, String clave, String nombreCompleto, String rol, String codigoCliente) {}

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, UsuarioInfo> usuarios = new ConcurrentHashMap<>();

    public UsuarioStore() {
        // Administrador con acceso completo a monitoreo, reglas, gráficas y reportes
        usuarios.put("admin", crearUsuario("admin", "lambda2026", "Administrador del Sistema", "ADMIN", null));

        // Analista antifraude: acceso de solo lectura a las páginas de administración
        usuarios.put("analista", crearUsuario("analista", "fraude123", "Analista Antifraude", "ANALISTA", null));

        // Usuario estándar (cliente de banca digital que realiza transacciones)
        usuarios.put("usuario", crearUsuario("usuario", "cliente123", "María Fernández (Cliente)", "CLIENTE", "C-001"));
        usuarios.put("maria", crearUsuario("maria", "123456", "María Fernández (Cliente)", "CLIENTE", "C-001"));
        usuarios.put("cliente", crearUsuario("cliente", "cliente123", "María Fernández (Cliente)", "CLIENTE", "C-001"));
    }

    private UsuarioInfo crearUsuario(String usuario, String clavePlano, String nombreCompleto, String rol, String codigoCliente) {
        return new UsuarioInfo(usuario, passwordEncoder.encode(clavePlano), nombreCompleto, rol, codigoCliente);
    }

    public Optional<UsuarioInfo> validar(String usuario, String clave) {
        UsuarioInfo info = usuario == null || clave == null ? null : usuarios.get(usuario);
        if (info != null && passwordEncoder.matches(clave, info.clave())) {
            return Optional.of(info);
        }
        return Optional.empty();
    }

    public Optional<UsuarioInfo> buscarPorUsuario(String usuario) {
        return usuario == null ? Optional.empty() : Optional.ofNullable(usuarios.get(usuario));
    }
}

package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.entity.Cliente;
import com.lambdashield.fraude.model.entity.Rol;
import com.lambdashield.fraude.model.entity.UsuarioRol;
import com.lambdashield.fraude.model.entity.UsuarioSistema;
import com.lambdashield.fraude.model.enums.EstadoUsuario;
import com.lambdashield.fraude.model.repository.ClienteRepository;
import com.lambdashield.fraude.model.repository.UsuarioRolRepository;
import com.lambdashield.fraude.model.repository.UsuarioSistemaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Autenticación contra usuario_sistema / usuario_rol / rol.
 *
 * Las credenciales y los roles viven en PostgreSQL; no existe un catálogo de
 * usuarios en memoria.
 */
@Service
public class AuthService {

    public record UsuarioSesion(
            String usuario,
            String nombreCompleto,
            String rol,
            String codigoCliente
    ) {}

    private final UsuarioSistemaRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioSistemaRepository usuarioRepository,
                       UsuarioRolRepository usuarioRolRepository,
                       ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<UsuarioSesion> autenticar(String usuario, String clave) {
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            return Optional.empty();
        }

        Optional<UsuarioSistema> encontrado = usuarioRepository.findByNombreUsuario(usuario.trim());
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        UsuarioSistema usuarioSistema = encontrado.get();
        if (usuarioSistema.getEstadoUsuario() != EstadoUsuario.ACTIVO) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(clave, usuarioSistema.getHashPassword())) {
            return Optional.empty();
        }

        usuarioSistema.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuarioSistema);

        String codigoRol = obtenerRolCodigo(usuarioSistema.getIdUsuario());
        String codigoCliente = obtenerCodigoCliente(usuarioSistema.getIdCliente());
        return Optional.of(new UsuarioSesion(
                usuarioSistema.getNombreUsuario(),
                usuarioSistema.getNombres(),
                codigoRol,
                codigoCliente
        ));
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioSesion> obtenerUsuarioInfo(String usuario) {
        return usuarioRepository.findByNombreUsuario(usuario)
                .map(u -> new UsuarioSesion(
                        u.getNombreUsuario(),
                        u.getNombres(),
                        obtenerRolCodigo(u.getIdUsuario()),
                        obtenerCodigoCliente(u.getIdCliente())
                ));
    }

    @Transactional(readOnly = true)
    public String obtenerNombreUsuario(String usuario) {
        return usuarioRepository.findByNombreUsuario(usuario)
                .map(UsuarioSistema::getNombres)
                .orElse(usuario);
    }

    @Transactional(readOnly = true)
    public String obtenerRol(String usuario) {
        return usuarioRepository.findByNombreUsuario(usuario)
                .map(u -> obtenerRolCodigo(u.getIdUsuario()))
                .orElse("CLIENTE");
    }

    @Transactional(readOnly = true)
    public String obtenerCodigoCliente(String usuario) {
        return usuarioRepository.findByNombreUsuario(usuario)
                .map(u -> obtenerCodigoCliente(u.getIdCliente()))
                .orElse(null);
    }

    private String obtenerRolCodigo(Long idUsuario) {
        List<String> codigos = usuarioRolRepository.findByUsuario_IdUsuario(idUsuario)
                .stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol != null && rol.isEsActivo())
                .map(Rol::getCodigoRol)
                .toList();

        // Si un usuario tuviera más de un rol, se aplica el de mayor privilegio.
        if (codigos.contains("ADMIN")) {
            return "ADMIN";
        }
        if (codigos.contains("ANALISTA")) {
            return "ANALISTA";
        }
        return "CLIENTE";
    }

    private String obtenerCodigoCliente(Long idCliente) {
        if (idCliente == null) {
            return null;
        }
        return clienteRepository.findById(idCliente)
                .map(c -> c.getCodigoCliente())
                .orElse(null);
    }
}

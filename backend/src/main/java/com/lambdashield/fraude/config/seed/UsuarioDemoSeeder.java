package com.lambdashield.fraude.config.seed;

import com.lambdashield.fraude.domain.entity.Rol;
import com.lambdashield.fraude.domain.entity.UsuarioRol;
import com.lambdashield.fraude.domain.entity.UsuarioSistema;
import com.lambdashield.fraude.domain.enums.EstadoUsuario;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.RolRepository;
import com.lambdashield.fraude.repository.UsuarioRolRepository;
import com.lambdashield.fraude.repository.UsuarioSistemaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Crea los usuarios y asignaciones de rol de demostración. */
@Component
public class UsuarioDemoSeeder {

    private final ClienteRepository clienteRepository;
    private final RolRepository rolRepository;
    private final UsuarioSistemaRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioDemoSeeder(ClienteRepository clienteRepository,
                             RolRepository rolRepository,
                             UsuarioSistemaRepository usuarioRepository,
                             UsuarioRolRepository usuarioRolRepository,
                             PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void seed() {
        crearUsuario("admin", "Administrador Lambda", "admin@lambda.local", "admin123", "ADMIN", null);
        crearUsuario("analista", "Analista Lambda", "analista@lambda.local", "analista123", "ANALISTA", null);
        crearUsuario("usuario", "María Fernández (Cliente)", "usuario@lambda.local", "cliente123", "CLIENTE", "C-001");
        crearUsuario("maria", "María Fernández", "maria@lambda.local", "maria123", "CLIENTE", "C-001");
        crearUsuario("cliente", "Jorge Salas", "cliente@lambda.local", "cliente123", "CLIENTE", "C-002");
    }

    private void crearUsuario(String nombreUsuario, String nombres, String correo, String clave,
                              String codigoRol, String codigoCliente) {
        UsuarioSistema usuario = usuarioRepository.findByNombreUsuario(nombreUsuario).orElseGet(() -> {
            Long idCliente = codigoCliente == null ? null
                    : clienteRepository.findByCodigoCliente(codigoCliente).map(c -> c.getIdCliente()).orElse(null);
            return usuarioRepository.save(UsuarioSistema.builder()
                    .nombreUsuario(nombreUsuario)
                    .nombres(nombres)
                    .correo(correo)
                    .hashPassword(passwordEncoder.encode(clave))
                    .estadoUsuario(EstadoUsuario.ACTIVO)
                    .fechaCreacion(OffsetDateTime.now())
                    .idCliente(idCliente)
                    .build());
        });

        Rol rol = rolRepository.findByCodigoRol(codigoRol).orElseThrow();
        boolean asignado = usuarioRolRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .stream()
                .anyMatch(asignacion -> asignacion.getRol().getIdRol().equals(rol.getIdRol()));
        if (!asignado) {
            usuarioRolRepository.save(UsuarioRol.builder()
                    .usuario(usuario)
                    .rol(rol)
                    .fechaAsignacion(OffsetDateTime.now())
                    .build());
        }
    }
}

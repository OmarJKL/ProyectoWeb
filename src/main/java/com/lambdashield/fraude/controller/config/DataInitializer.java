package com.lambdashield.fraude.controller.config;

import com.lambdashield.fraude.model.entity.Canal;
import com.lambdashield.fraude.model.entity.Cliente;
import com.lambdashield.fraude.model.entity.Cuenta;
import com.lambdashield.fraude.model.entity.Dispositivo;
import com.lambdashield.fraude.model.entity.ReglaFraude;
import com.lambdashield.fraude.model.entity.Rol;
import com.lambdashield.fraude.model.entity.TipoTransaccion;
import com.lambdashield.fraude.model.entity.UsuarioRol;
import com.lambdashield.fraude.model.entity.UsuarioSistema;
import com.lambdashield.fraude.model.enums.EstadoCuenta;
import com.lambdashield.fraude.model.enums.EstadoUsuario;
import com.lambdashield.fraude.model.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.model.enums.NivelRiesgo;
import com.lambdashield.fraude.model.enums.TipoCuenta;
import com.lambdashield.fraude.model.enums.TipoDispositivo;
import com.lambdashield.fraude.model.repository.CanalRepository;
import com.lambdashield.fraude.model.repository.ClienteRepository;
import com.lambdashield.fraude.model.repository.CuentaRepository;
import com.lambdashield.fraude.model.repository.DispositivoRepository;
import com.lambdashield.fraude.model.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.model.repository.ReglaFraudeRepository;
import com.lambdashield.fraude.model.repository.RolRepository;
import com.lambdashield.fraude.model.repository.TipoTransaccionRepository;
import com.lambdashield.fraude.model.repository.UsuarioRolRepository;
import com.lambdashield.fraude.model.repository.UsuarioSistemaRepository;
import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.service.ClienteService;
import com.lambdashield.fraude.model.service.TransaccionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Carga inicial de datos de demostración en PostgreSQL.
 *
 * Es idempotente: si los catálogos o los clientes ya existen no se duplican.
 * Sirve tanto para el arranque normal como para las pruebas con H2.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private record ReglaSeed(String codigo, String nombre, String descripcion, int peso) {}
    private record DemoTransaccion(String cliente, double monto, String categoria, int hora,
                                   String pais, String dispositivo, int operaciones,
                                   String beneficiario, String ip) {}

    private static final List<ReglaSeed> REGLAS = List.of(
            new ReglaSeed("RF001", "Monto inusual",
                    "Puntúa de forma gradual cuando el monto supera el promedio histórico del cliente.", 25),
            new ReglaSeed("RF002", "Dispositivo nuevo",
                    "Puntúa cuando la operación se realiza desde un dispositivo no registrado.", 15),
            new ReglaSeed("RF003", "IP nueva",
                    "Puntúa cuando la operación proviene de una dirección IP no habitual.", 10),
            new ReglaSeed("RF004", "País nuevo",
                    "Puntúa operaciones originadas en países o jurisdicciones poco frecuentes.", 30),
            new ReglaSeed("RF005", "Horario inusual",
                    "Puntúa operaciones realizadas en la madrugada (00:00-04:59).", 10),
            new ReglaSeed("RF006", "Alta frecuencia",
                    "Puntúa ráfagas de operaciones en la última hora, típicas de pruebas de tarjeta.", 25),
            new ReglaSeed("RF007", "Viaje imposible",
                    "Heurística simplificada basada en país, frecuencia e IP.", 40),
            new ReglaSeed("RF008", "Beneficiario nuevo con monto alto",
                    "Puntúa cuando el beneficiario es nuevo y el monto supera el umbral.", 20)
    );

    private static final List<DemoTransaccion> DEMOS = List.of(
            new DemoTransaccion("C-001", 150.00, "servicios", 14, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-001", 85.50, "retail", 9, "local", "reconocido", 1, "recurrente", "conocida"),
            new DemoTransaccion("C-002", 320.00, "electronica", 18, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-003", 95.00, "restaurante", 20, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-001", 210.00, "retail", 11, "regional", "nuevo", 2, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 750.00, "electronica", 2, "local", "reconocido", 3, "recurrente", "conocida"),
            new DemoTransaccion("C-003", 600.00, "servicios", 3, "regional", "nuevo", 4, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 1850.00, "cripto", 1, "alto_riesgo", "nuevo", 5, "nuevo", "nueva"),
            new DemoTransaccion("C-003", 3200.00, "casino", 4, "alto_riesgo", "nuevo", 6, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 8900.00, "cripto", 0, "alto_riesgo", "nuevo", 6, "nuevo", "nueva")
    );

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final RolRepository rolRepository;
    private final UsuarioSistemaRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final CanalRepository canalRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final ReglaFraudeRepository reglaFraudeRepository;
    private final EvaluacionFraudeRepository evaluacionFraudeRepository;
    private final TransaccionService transaccionService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ClienteRepository clienteRepository,
                           CuentaRepository cuentaRepository,
                           DispositivoRepository dispositivoRepository,
                           RolRepository rolRepository,
                           UsuarioSistemaRepository usuarioRepository,
                           UsuarioRolRepository usuarioRolRepository,
                           CanalRepository canalRepository,
                           TipoTransaccionRepository tipoTransaccionRepository,
                           ReglaFraudeRepository reglaFraudeRepository,
                           EvaluacionFraudeRepository evaluacionFraudeRepository,
                           TransaccionService transaccionService,
                           PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.canalRepository = canalRepository;
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.reglaFraudeRepository = reglaFraudeRepository;
        this.evaluacionFraudeRepository = evaluacionFraudeRepository;
        this.transaccionService = transaccionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedClientes();
        seedCatalogos();
        seedReglas();
        seedUsuarios();
        seedEvaluacionesDemo();
    }

    private void seedRoles() {
        crearRol("ADMIN", "Administrador", "Acceso total al sistema");
        crearRol("ANALISTA", "Analista antifraude", "Consulta y monitoreo de operaciones");
        crearRol("CLIENTE", "Cliente", "Portal de banca en línea");
    }

    private void crearRol(String codigo, String nombre, String descripcion) {
        rolRepository.findByCodigoRol(codigo).orElseGet(() -> rolRepository.save(Rol.builder()
                .codigoRol(codigo)
                .nombreRol(nombre)
                .descripcion(descripcion)
                .esActivo(true)
                .build()));
    }

    private void seedClientes() {
        crearCliente("C-001", "DNI", "10000001", "María", "Fernández", "Rojas",
                "maria.fernandez@lambda.local", "999111222", new BigDecimal("180.00"));
        crearCliente("C-002", "DNI", "10000002", "Jorge", "Salas", "Mendoza",
                "jorge.salas@lambda.local", "999333444", new BigDecimal("950.00"));
        crearCliente("C-003", "DNI", "10000003", "Ana", "Torres", "Vega",
                "ana.torres@lambda.local", "999555666", new BigDecimal("420.00"));

        Cliente c1 = clienteRepository.findByCodigoCliente("C-001").orElseThrow();
        Cliente c2 = clienteRepository.findByCodigoCliente("C-002").orElseThrow();
        Cliente c3 = clienteRepository.findByCodigoCliente("C-003").orElseThrow();

        crearCuenta(c1, "0011-0145-0200847291", new BigDecimal("4850.00"));
        crearCuenta(c2, "0011-0145-0200847292", new BigDecimal("12500.00"));
        crearCuenta(c3, "0011-0145-0200847293", new BigDecimal("780.00"));

        crearDispositivos(c1, List.of("Chrome|Windows", "Edge|Windows"));
        crearDispositivos(c2, List.of("Chrome|Android", "Chrome|Windows"));
        crearDispositivos(c3, List.of("Firefox|Windows", "Safari|macOS"));
    }

    private void crearCliente(String codigo, String tipoDocumento, String numeroDocumento,
                              String nombres, String apellidoPaterno, String apellidoMaterno,
                              String correo, String telefono, BigDecimal promedioGasto) {
        if (clienteRepository.findByCodigoCliente(codigo).isPresent()) {
            return;
        }
        clienteRepository.save(Cliente.builder()
                .codigoCliente(codigo)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .nombres(nombres)
                .apellidoPaterno(apellidoPaterno)
                .apellidoMaterno(apellidoMaterno)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .correoElectronico(correo)
                .numeroTelefono(telefono)
                .promedioGasto(promedioGasto)
                .esActivo(true)
                .build());
    }

    private void crearCuenta(Cliente cliente, String numeroCuenta, BigDecimal saldo) {
        if (cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente()).isPresent()) {
            return;
        }
        cuentaRepository.save(Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .tipoCuenta(TipoCuenta.AHORROS)
                .codigoMoneda("PEN")
                .saldoDisponible(saldo)
                .estadoCuenta(EstadoCuenta.ACTIVA)
                .fechaApertura(LocalDate.now().minusYears(2))
                .fechaCreacion(OffsetDateTime.now())
                .build());
    }

    private void crearDispositivos(Cliente cliente, List<String> normalizados) {
        if (!dispositivoRepository.findByCliente_IdCliente(cliente.getIdCliente()).isEmpty()) {
            return;
        }
        for (String normalizado : normalizados) {
            String[] partes = normalizado.split("\\|", 2);
            String navegador = partes[0];
            String sistema = partes[1];
            dispositivoRepository.save(Dispositivo.builder()
                    .cliente(cliente)
                    .huellaDispositivo(ClienteService.huella(cliente.getCodigoCliente(), normalizado))
                    .tipoDispositivo("Android".equals(sistema) || "iOS".equals(sistema)
                            ? TipoDispositivo.MOVIL : TipoDispositivo.PC)
                    .sistemaOperativo(sistema)
                    .navegador(navegador)
                    .direccionIp("127.0.0.1")
                    .esConocido(true)
                    .fechaPrimerUso(OffsetDateTime.now().minusMonths(6))
                    .fechaUltimoUso(OffsetDateTime.now())
                    .build());
        }
    }

    private void seedCatalogos() {
        if (canalRepository.findByCodigoCanal("WEB").isEmpty()) {
            canalRepository.save(Canal.builder()
                    .codigoCanal("WEB")
                    .nombreCanal("Banca por internet")
                    .esActivo(true)
                    .build());
        }

        for (String categoria : List.of("servicios", "retail", "electronica", "restaurante",
                "cripto", "casino", "otros")) {
            if (tipoTransaccionRepository.findByCodigoTipo(categoria).isEmpty()) {
                tipoTransaccionRepository.save(TipoTransaccion.builder()
                        .codigoTipo(categoria)
                        .nombreTipo(capitalizar(categoria))
                        .naturaleza(NaturalezaTransaccion.DEBITO)
                        .esActivo(true)
                        .build());
            }
        }
    }

    private void seedReglas() {
        for (ReglaSeed seed : REGLAS) {
            if (reglaFraudeRepository.findByCodigoRegla(seed.codigo()).isPresent()) {
                continue;
            }
            reglaFraudeRepository.save(ReglaFraude.builder()
                    .codigoRegla(seed.codigo())
                    .nombreRegla(seed.nombre())
                    .descripcion(seed.descripcion())
                    .nivelRiesgo(NivelRiesgo.ALTO)
                    .peso(BigDecimal.valueOf(seed.peso()))
                    .versionRegla(1)
                    .esActiva(true)
                    .fechaInicioVigencia(OffsetDateTime.now())
                    .build());
        }
    }

    private void seedUsuarios() {
        crearUsuario("admin", "Administrador Lambda", "admin@lambda.local", "admin123", "ADMIN", null);
        crearUsuario("analista", "Analista Lambda", "analista@lambda.local", "analista123", "ANALISTA", null);
        crearUsuario("usuario", "María Fernández (Cliente)", "usuario@lambda.local", "cliente123",
                "CLIENTE", "C-001");
        crearUsuario("maria", "María Fernández", "maria@lambda.local", "maria123", "CLIENTE", "C-001");
        crearUsuario("cliente", "Jorge Salas", "cliente@lambda.local", "cliente123", "CLIENTE", "C-002");
    }

    private void crearUsuario(String nombreUsuario, String nombres, String correo, String clave,
                              String codigoRol, String codigoCliente) {
        UsuarioSistema usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseGet(() -> {
                    Long idCliente = codigoCliente == null ? null
                            : clienteRepository.findByCodigoCliente(codigoCliente)
                                    .map(Cliente::getIdCliente)
                                    .orElse(null);
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
                .anyMatch(ur -> ur.getRol().getIdRol().equals(rol.getIdRol()));
        if (!asignado) {
            usuarioRolRepository.save(UsuarioRol.builder()
                    .usuario(usuario)
                    .rol(rol)
                    .fechaAsignacion(OffsetDateTime.now())
                    .build());
        }
    }

    private void seedEvaluacionesDemo() {
        if (evaluacionFraudeRepository.count() > 0) {
            return;
        }
        for (DemoTransaccion demo : DEMOS) {
            transaccionService.crearYEvaluar(new TransaccionCrearRequest(
                    demo.cliente(), demo.monto(), demo.categoria(), demo.hora(), demo.pais(),
                    demo.dispositivo(), demo.operaciones(), demo.beneficiario(), demo.ip()
            ));
        }
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}

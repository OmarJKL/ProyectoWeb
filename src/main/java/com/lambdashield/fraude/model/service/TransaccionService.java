package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.entity.Beneficiario;
import com.lambdashield.fraude.model.entity.Canal;
import com.lambdashield.fraude.model.entity.Cliente;
import com.lambdashield.fraude.model.entity.Cuenta;
import com.lambdashield.fraude.model.entity.Dispositivo;
import com.lambdashield.fraude.model.entity.EvaluacionFraude;
import com.lambdashield.fraude.model.entity.EvaluacionRegla;
import com.lambdashield.fraude.model.entity.ReglaFraude;
import com.lambdashield.fraude.model.entity.TipoTransaccion;
import com.lambdashield.fraude.model.entity.Transaccion;
import com.lambdashield.fraude.model.enums.DecisionFraude;
import com.lambdashield.fraude.model.enums.EstadoTransaccion;
import com.lambdashield.fraude.model.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.model.enums.NivelRiesgo;
import com.lambdashield.fraude.model.enums.TipoDispositivo;
import com.lambdashield.fraude.model.repository.BeneficiarioRepository;
import com.lambdashield.fraude.model.repository.CanalRepository;
import com.lambdashield.fraude.model.repository.ClienteRepository;
import com.lambdashield.fraude.model.repository.DispositivoRepository;
import com.lambdashield.fraude.model.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.model.repository.EvaluacionReglaRepository;
import com.lambdashield.fraude.model.repository.ReglaFraudeRepository;
import com.lambdashield.fraude.model.repository.TipoTransaccionRepository;
import com.lambdashield.fraude.model.repository.TransaccionRepository;
import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.response.FactorResponse;
import com.lambdashield.fraude.model.response.KpisResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio principal de evaluaciones.
 *
 * La evaluación, sus factores, la transacción y el cliente se persisten en las
 * tablas PostgreSQL del proyecto. No se conserva historial en memoria.
 */
@Service
public class TransaccionService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> FACTOR_A_REGLA = Map.of(
            "monto", "RF001",
            "dispositivo", "RF002",
            "ip", "RF003",
            "ubicacion", "RF004",
            "horario", "RF005",
            "velocidad", "RF006",
            "viaje_imposible", "RF007",
            "beneficiario", "RF008"
    );

    private static final Map<String, String> REGLA_A_FACTOR = Map.of(
            "RF001", "monto",
            "RF002", "dispositivo",
            "RF003", "ip",
            "RF004", "ubicacion",
            "RF005", "horario",
            "RF006", "velocidad",
            "RF007", "viaje_imposible",
            "RF008", "beneficiario"
    );

    private final DeteccionFraudeService deteccionFraudeService;
    private final ClienteService clienteService;
    private final ClienteRepository clienteRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final CanalRepository canalRepository;
    private final DispositivoRepository dispositivoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final TransaccionRepository transaccionRepository;
    private final EvaluacionFraudeRepository evaluacionFraudeRepository;
    private final EvaluacionReglaRepository evaluacionReglaRepository;
    private final ReglaFraudeRepository reglaFraudeRepository;

    public TransaccionService(DeteccionFraudeService deteccionFraudeService,
                              ClienteService clienteService,
                              ClienteRepository clienteRepository,
                              TipoTransaccionRepository tipoTransaccionRepository,
                              CanalRepository canalRepository,
                              DispositivoRepository dispositivoRepository,
                              BeneficiarioRepository beneficiarioRepository,
                              TransaccionRepository transaccionRepository,
                              EvaluacionFraudeRepository evaluacionFraudeRepository,
                              EvaluacionReglaRepository evaluacionReglaRepository,
                              ReglaFraudeRepository reglaFraudeRepository) {
        this.deteccionFraudeService = deteccionFraudeService;
        this.clienteService = clienteService;
        this.clienteRepository = clienteRepository;
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.canalRepository = canalRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.transaccionRepository = transaccionRepository;
        this.evaluacionFraudeRepository = evaluacionFraudeRepository;
        this.evaluacionReglaRepository = evaluacionReglaRepository;
        this.reglaFraudeRepository = reglaFraudeRepository;
    }

    @Transactional
    public EvaluacionResponse crearYEvaluar(TransaccionCrearRequest request) {
        Cliente cliente = obtenerCliente(request.cliente());
        return crearYEvaluar(request, OffsetDateTime.now(), cliente, "127.0.0.1", null);
    }

    /**
     * Evaluación para el portal del cliente: hora, frecuencia, dispositivo e IP
     * se derivan en el servidor y no se confía en los valores del body.
     */
    @Transactional
    public EvaluacionResponse crearYEvaluarCliente(TransaccionCrearRequest request, String codigoCliente,
                                                   String userAgent, String ipRemota) {
        Cliente cliente = obtenerCliente(codigoCliente);
        OffsetDateTime fechaHora = OffsetDateTime.now();

        long operacionesUltimaHora = evaluacionFraudeRepository
                .countByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteAndFechaEvaluacionAfter(
                        codigoCliente, fechaHora.minusHours(1));

        String dispositivo = clienteService.esDispositivoConocido(codigoCliente, userAgent)
                ? "reconocido"
                : "nuevo";
        String ip = clienteService.esIpConocida(codigoCliente, ipRemota)
                ? "conocida"
                : "nueva";

        TransaccionCrearRequest derivada = new TransaccionCrearRequest(
                codigoCliente,
                request.monto(),
                request.categoria(),
                fechaHora.getHour(),
                request.pais(),
                dispositivo,
                (int) Math.min(Integer.MAX_VALUE, operacionesUltimaHora),
                request.beneficiario(),
                ip
        );

        return crearYEvaluar(derivada, fechaHora, cliente, ipRemota, userAgent);
    }

    private EvaluacionResponse crearYEvaluar(TransaccionCrearRequest request, OffsetDateTime fechaHora,
                                             Cliente cliente, String direccionIp, String userAgent) {
        double promedio = cliente.getPromedioGasto() != null
                ? cliente.getPromedioGasto().doubleValue()
                : 0.0;

        List<FactorResponse> factores = deteccionFraudeService.evaluar(request, promedio);
        int puntaje = deteccionFraudeService.calcularPuntajeTotal(factores);
        String nivel = deteccionFraudeService.clasificarNivel(puntaje);

        Cuenta cuenta = clienteService.obtenerCuentaPrincipal(cliente);
        TipoTransaccion tipoTransaccion = obtenerTipoTransaccion(request.categoria());
        Canal canal = obtenerCanal();
        Dispositivo dispositivo = resolverDispositivo(cliente, request.dispositivo(), userAgent);
        Beneficiario beneficiario = resolverBeneficiario(cliente, request.beneficiario());

        OffsetDateTime fechaTransaccion = fechaHora
                .withHour(request.hora())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Transaccion transaccion = Transaccion.builder()
                .codigoTransaccion(UUID.randomUUID())
                .cuenta(cuenta)
                .beneficiario(beneficiario)
                .tipoTransaccion(tipoTransaccion)
                .canal(canal)
                .dispositivo(dispositivo)
                .montoTransaccion(BigDecimal.valueOf(request.monto()).setScale(2, RoundingMode.HALF_UP))
                .codigoMoneda("PEN")
                .fechaHoraTransaccion(fechaTransaccion)
                .saldoAnterior(cuenta.getSaldoDisponible())
                .saldoPosterior(cuenta.getSaldoDisponible())
                .direccionIp(direccionIp)
                .codigoPais(codigoPais(request.pais()))
                .ciudad("Lima")
                .estadoTransaccion(estadoTransaccion(nivel))
                .fechaRegistro(OffsetDateTime.now())
                .build();
        transaccionRepository.save(transaccion);

        EvaluacionFraude evaluacion = EvaluacionFraude.builder()
                .transaccion(transaccion)
                .fraudeScore(BigDecimal.valueOf(puntaje).setScale(2, RoundingMode.HALF_UP))
                .nivelRiesgo(nivelRiesgo(nivel))
                .decision(decision(nivel))
                .esPosibleFraude("alto".equals(nivel) || "critico".equals(nivel))
                .modeloUtilizado("Reglas RF001-RF008")
                .versionModelo("1.0")
                .fechaEvaluacion(fechaHora)
                .duracionMs(0)
                .esActiva(true)
                .build();
        evaluacionFraudeRepository.save(evaluacion);

        for (FactorResponse factor : factores) {
            ReglaFraude regla = reglaFraudeRepository
                    .findByCodigoRegla(FACTOR_A_REGLA.get(factor.id()))
                    .orElse(null);
            if (regla == null) {
                continue;
            }
            EvaluacionRegla evaluacionRegla = EvaluacionRegla.builder()
                    .evaluacion(evaluacion)
                    .regla(regla)
                    .seDisparo(factor.puntaje() > 0)
                    .valorDetectado(truncar(factor.motivo(), 250))
                    .pesoAplicado(BigDecimal.valueOf(factor.puntaje()).setScale(2, RoundingMode.HALF_UP))
                    .build();
            evaluacionReglaRepository.save(evaluacionRegla);
        }

        return toResponse(evaluacion, transaccion, cliente, factores);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listarTodas() {
        return evaluacionFraudeRepository.findByEsActivaTrueOrderByFechaEvaluacionDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listarPorClienteCodigo(String codigoCliente) {
        return evaluacionFraudeRepository
                .findByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteOrderByFechaEvaluacionDesc(codigoCliente)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public KpisResponse calcularKpis() {
        List<EvaluacionResponse> todas = listarTodas();
        long total = todas.size();
        long aprobadas = todas.stream().filter(e -> "bajo".equals(e.nivel())).count();
        long monitoreadas = todas.stream().filter(e -> "medio".equals(e.nivel())).count();
        long enRevision = todas.stream().filter(e -> "alto".equals(e.nivel())).count();
        long bloqueadas = todas.stream().filter(e -> "critico".equals(e.nivel())).count();
        double promedio = total > 0
                ? todas.stream().mapToInt(EvaluacionResponse::puntaje).average().orElse(0.0)
                : 0.0;
        return new KpisResponse(total, aprobadas, monitoreadas, enRevision, bloqueadas, promedio);
    }

    /**
     * Soft delete: la fila permanece en la base de datos, pero se marca como
     * inactiva y la transacción se registra como REVERSADA.
     */
    @Transactional
    public void desactivarEvaluacion(Long idEvaluacion) {
        EvaluacionFraude evaluacion = evaluacionFraudeRepository
                .findByIdEvaluacionAndEsActivaTrue(idEvaluacion)
                .orElseThrow(() -> new EvaluacionNoEncontradaException(idEvaluacion));

        evaluacion.setEsActiva(false);
        evaluacion.setFechaDesactivacion(OffsetDateTime.now());
        evaluacion.getTransaccion().setEstadoTransaccion(EstadoTransaccion.REVERSADA);
        evaluacionFraudeRepository.save(evaluacion);
        transaccionRepository.save(evaluacion.getTransaccion());
    }

    private Cliente obtenerCliente(String codigoCliente) {
        return clienteRepository.findByCodigoCliente(codigoCliente)
                .orElseThrow(() -> new ClienteNoEncontradoException(codigoCliente));
    }

    private TipoTransaccion obtenerTipoTransaccion(String categoria) {
        String codigo = normalizarCategoria(categoria);
        return tipoTransaccionRepository.findByCodigoTipo(codigo)
                .orElseGet(() -> tipoTransaccionRepository.save(TipoTransaccion.builder()
                        .codigoTipo(codigo)
                        .nombreTipo(capitalizar(codigo))
                        .naturaleza(NaturalezaTransaccion.DEBITO)
                        .esActivo(true)
                        .build()));
    }

    private Canal obtenerCanal() {
        return canalRepository.findByCodigoCanal("WEB")
                .orElseGet(() -> canalRepository.save(Canal.builder()
                        .codigoCanal("WEB")
                        .nombreCanal("Banca por internet")
                        .esActivo(true)
                        .build()));
    }

    private Dispositivo resolverDispositivo(Cliente cliente, String tipoSolicitado, String userAgent) {
        if ("reconocido".equalsIgnoreCase(tipoSolicitado)) {
            return dispositivoRepository.findByCliente_IdClienteAndEsConocidoTrue(cliente.getIdCliente())
                    .stream()
                    .findFirst()
                    .orElseGet(() -> crearDispositivo(cliente,
                            ClienteService.normalizarDispositivo(userAgent), true));
        }
        return crearDispositivo(cliente,
                userAgent != null ? ClienteService.normalizarDispositivo(userAgent) : "Nuevo|Desconocido",
                false);
    }

    private Dispositivo crearDispositivo(Cliente cliente, String normalizado, boolean conocido) {
        String[] partes = normalizado.split("\\|", 2);
        String navegador = partes.length > 0 ? partes[0] : "Otro";
        String sistema = partes.length > 1 ? partes[1] : "Otro";
        TipoDispositivo tipo = "Android".equals(sistema) || "iOS".equals(sistema)
                ? TipoDispositivo.MOVIL
                : TipoDispositivo.PC;
        OffsetDateTime ahora = OffsetDateTime.now();

        return dispositivoRepository.save(Dispositivo.builder()
                .cliente(cliente)
                .huellaDispositivo(ClienteService.huella(cliente.getCodigoCliente(), normalizado)
                        + "|" + UUID.randomUUID())
                .tipoDispositivo(tipo)
                .sistemaOperativo(sistema)
                .navegador(navegador)
                .esConocido(conocido)
                .fechaPrimerUso(ahora)
                .fechaUltimoUso(ahora)
                .build());
    }

    private Beneficiario resolverBeneficiario(Cliente cliente, String tipoBeneficiario) {
        boolean nuevo = "nuevo".equalsIgnoreCase(tipoBeneficiario);
        String nombre = nuevo ? "Destinatario nuevo" : "Destinatario recurrente";
        return beneficiarioRepository
                .findFirstByCliente_IdClienteAndNombreBeneficiario(cliente.getIdCliente(), nombre)
                .orElseGet(() -> beneficiarioRepository.save(Beneficiario.builder()
                        .cliente(cliente)
                        .nombreBeneficiario(nombre)
                        .entidadFinanciera("Lambda Bank")
                        .codigoPais("PE")
                        .esConocido(!nuevo)
                        .fechaPrimerUso(OffsetDateTime.now())
                        .fechaUltimoUso(OffsetDateTime.now())
                        .esActivo(true)
                        .build()));
    }

    private EvaluacionResponse toResponse(EvaluacionFraude evaluacion) {
        List<FactorResponse> factores = evaluacionReglaRepository
                .findByEvaluacion_IdEvaluacion(evaluacion.getIdEvaluacion())
                .stream()
                .map(this::toFactor)
                .toList();
        Transaccion transaccion = evaluacion.getTransaccion();
        Cliente cliente = transaccion.getCuenta().getCliente();
        return toResponse(evaluacion, transaccion, cliente, factores);
    }

    private EvaluacionResponse toResponse(EvaluacionFraude evaluacion, Transaccion transaccion,
                                          Cliente cliente, List<FactorResponse> factores) {
        String nivel = evaluacion.getNivelRiesgo().name().toLowerCase(Locale.ROOT);
        return new EvaluacionResponse(
                String.format("TR-%05d", evaluacion.getIdEvaluacion()),
                cliente.getCodigoCliente(),
                ClienteService.nombreCompleto(cliente),
                transaccion.getMontoTransaccion().doubleValue(),
                transaccion.getTipoTransaccion().getCodigoTipo(),
                paisDesdeCodigo(transaccion.getCodigoPais()),
                transaccion.getFechaHoraTransaccion().getHour(),
                evaluacion.getFraudeScore().intValue(),
                nivel,
                deteccionFraudeService.determinarEstado(nivel),
                deteccionFraudeService.obtenerIconoEstado(nivel),
                deteccionFraudeService.obtenerColorNivel(nivel),
                evaluacion.getFechaEvaluacion().format(FORMATTER),
                evaluacion.getFechaEvaluacion().toLocalDateTime(),
                factores,
                evaluacion.getIdEvaluacion()
        );
    }

    private FactorResponse toFactor(EvaluacionRegla evaluacionRegla) {
        ReglaFraude regla = evaluacionRegla.getRegla();
        int maximo = regla.getPeso() != null ? regla.getPeso().intValue() : 0;
        int puntaje = evaluacionRegla.getPesoAplicado() != null
                ? evaluacionRegla.getPesoAplicado().intValue()
                : 0;
        return new FactorResponse(
                REGLA_A_FACTOR.getOrDefault(regla.getCodigoRegla(), regla.getCodigoRegla().toLowerCase(Locale.ROOT)),
                regla.getNombreRegla(),
                puntaje,
                maximo,
                evaluacionRegla.getValorDetectado()
        );
    }

    private static NivelRiesgo nivelRiesgo(String nivel) {
        return switch (nivel) {
            case "bajo" -> NivelRiesgo.BAJO;
            case "medio" -> NivelRiesgo.MEDIO;
            case "alto" -> NivelRiesgo.ALTO;
            default -> NivelRiesgo.CRITICO;
        };
    }

    private static DecisionFraude decision(String nivel) {
        return switch (nivel) {
            case "bajo" -> DecisionFraude.APROBAR;
            case "medio" -> DecisionFraude.MONITOREAR;
            case "alto" -> DecisionFraude.RETENER;
            default -> DecisionFraude.BLOQUEAR;
        };
    }

    private static EstadoTransaccion estadoTransaccion(String nivel) {
        return switch (nivel) {
            case "bajo", "medio" -> EstadoTransaccion.APROBADA;
            case "alto" -> EstadoTransaccion.RETENIDA;
            default -> EstadoTransaccion.BLOQUEADA;
        };
    }

    private static String codigoPais(String pais) {
        return switch (pais) {
            case "local" -> "PE";
            case "regional" -> "CL";
            default -> "XX";
        };
    }

    private static String paisDesdeCodigo(String codigo) {
        if ("PE".equals(codigo)) {
            return "local";
        }
        if ("CL".equals(codigo) || "CO".equals(codigo) || "EC".equals(codigo)) {
            return "regional";
        }
        return "alto_riesgo";
    }

    private static String normalizarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return "otros";
        }
        return categoria.trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    private static String truncar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}

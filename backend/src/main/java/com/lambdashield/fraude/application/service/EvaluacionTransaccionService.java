package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.domain.entity.EvaluacionRegla;
import com.lambdashield.fraude.domain.entity.ReglaFraude;
import com.lambdashield.fraude.domain.entity.Transaccion;
import com.lambdashield.fraude.application.command.EvaluarTransaccionCommand;
import com.lambdashield.fraude.domain.enums.DecisionFraude;
import com.lambdashield.fraude.domain.enums.EstadoTransaccion;
import com.lambdashield.fraude.domain.enums.NivelRiesgo;
import com.lambdashield.fraude.domain.fraud.FactorEvaluado;
import com.lambdashield.fraude.domain.fraud.FactorRegla;
import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.application.mapper.EvaluacionResultadoMapper;
import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.FactorResultado;
import com.lambdashield.fraude.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.repository.EvaluacionReglaRepository;
import com.lambdashield.fraude.repository.ReglaFraudeRepository;
import com.lambdashield.fraude.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio principal de evaluaciones.
 *
 * La evaluación, sus factores, la transacción y el cliente se persisten en las
 * tablas PostgreSQL del proyecto. No se conserva historial en memoria.
 */
@Service
public class EvaluacionTransaccionService {

    private final DeteccionFraudeService deteccionFraudeService;
    private final ClienteService clienteService;
    private final ClienteContextService clienteContextService;
    private final TransaccionReferenciasResolver referenciasResolver;
    private final TransaccionRepository transaccionRepository;
    private final EvaluacionFraudeRepository evaluacionFraudeRepository;
    private final EvaluacionReglaRepository evaluacionReglaRepository;
    private final ReglaFraudeRepository reglaFraudeRepository;
    private final EvaluacionResultadoMapper resultadoMapper;

    public EvaluacionTransaccionService(DeteccionFraudeService deteccionFraudeService,
                              ClienteService clienteService,
                              ClienteContextService clienteContextService,
                              TransaccionReferenciasResolver referenciasResolver,
                              TransaccionRepository transaccionRepository,
                              EvaluacionFraudeRepository evaluacionFraudeRepository,
                              EvaluacionReglaRepository evaluacionReglaRepository,
                              ReglaFraudeRepository reglaFraudeRepository,
                              EvaluacionResultadoMapper resultadoMapper) {
        this.deteccionFraudeService = deteccionFraudeService;
        this.clienteService = clienteService;
        this.clienteContextService = clienteContextService;
        this.referenciasResolver = referenciasResolver;
        this.transaccionRepository = transaccionRepository;
        this.evaluacionFraudeRepository = evaluacionFraudeRepository;
        this.evaluacionReglaRepository = evaluacionReglaRepository;
        this.reglaFraudeRepository = reglaFraudeRepository;
        this.resultadoMapper = resultadoMapper;
    }

    @Transactional
    public EvaluacionResultado crearYEvaluar(EvaluarTransaccionCommand request) {
        Cliente cliente = obtenerCliente(request.cliente());
        return crearYEvaluar(request, OffsetDateTime.now(), cliente, "127.0.0.1", null);
    }

    /**
     * Evaluación para el portal del cliente: hora, frecuencia, dispositivo e IP
     * se derivan en el servidor y no se confía en los valores del body.
     */
    @Transactional
    public EvaluacionResultado crearYEvaluarCliente(EvaluarTransaccionCommand request, String codigoCliente,
                                                   String userAgent, String ipRemota) {
        Cliente cliente = obtenerCliente(codigoCliente);
        OffsetDateTime fechaHora = OffsetDateTime.now();

        long operacionesUltimaHora = evaluacionFraudeRepository
                .countByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteAndFechaEvaluacionAfter(
                        codigoCliente, fechaHora.minusHours(1));

        String dispositivo = clienteContextService.esDispositivoConocido(codigoCliente, userAgent)
                ? "reconocido"
                : "nuevo";
        String ip = clienteContextService.esIpConocida(codigoCliente, ipRemota)
                ? "conocida"
                : "nueva";

        EvaluarTransaccionCommand derivada = new EvaluarTransaccionCommand(
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

    private EvaluacionResultado crearYEvaluar(EvaluarTransaccionCommand request, OffsetDateTime fechaHora,
                                             Cliente cliente, String direccionIp, String userAgent) {
        double promedio = cliente.getPromedioGasto() != null
                ? cliente.getPromedioGasto().doubleValue()
                : 0.0;

        TransaccionEvaluable transaccionEvaluable = new TransaccionEvaluable(
                request.monto(), request.hora(), request.pais(), request.dispositivo(),
                request.operacionesUltimaHora(), request.beneficiario(), request.ip());
        List<FactorEvaluado> factoresEvaluados = deteccionFraudeService.evaluar(transaccionEvaluable, promedio);
        int puntaje = deteccionFraudeService.calcularPuntajeTotal(factoresEvaluados);
        String nivel = deteccionFraudeService.clasificarNivel(puntaje);

        Cuenta cuenta = clienteService.obtenerCuentaPrincipal(cliente);
        TransaccionReferenciasResolver.Referencias referencias =
                referenciasResolver.resolver(cliente, request, userAgent);

        OffsetDateTime fechaTransaccion = fechaHora
                .withHour(request.hora())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Transaccion transaccion = Transaccion.builder()
                .codigoTransaccion(UUID.randomUUID())
                .cuenta(cuenta)
                .beneficiario(referencias.beneficiario())
                .tipoTransaccion(referencias.tipoTransaccion())
                .canal(referencias.canal())
                .dispositivo(referencias.dispositivo())
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

        for (FactorEvaluado factor : factoresEvaluados) {
            FactorRegla catalogo = FactorRegla.porId(factor.id()).orElse(null);
            if (catalogo == null) continue;
            ReglaFraude regla = reglaFraudeRepository
                    .findByCodigoRegla(catalogo.codigo())
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

        List<FactorResultado> factores = factoresEvaluados.stream()
                .map(resultadoMapper::toFactorResultado)
                .toList();
        return resultadoMapper.toResultado(evaluacion, factores);
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
        return clienteService.buscarEntidad(codigoCliente)
                .orElseThrow(() -> new ClienteNoEncontradoException(codigoCliente));
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

    private static String truncar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}

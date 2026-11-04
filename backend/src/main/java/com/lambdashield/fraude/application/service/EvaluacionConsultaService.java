package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.mapper.EvaluacionResultadoMapper;
import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.FactorResultado;
import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.repository.EvaluacionReglaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Casos de uso de consulta del historial de evaluaciones. */
@Service
public class EvaluacionConsultaService {

    private final EvaluacionFraudeRepository evaluacionRepository;
    private final EvaluacionReglaRepository evaluacionReglaRepository;
    private final EvaluacionResultadoMapper mapper;

    public EvaluacionConsultaService(EvaluacionFraudeRepository evaluacionRepository,
                                     EvaluacionReglaRepository evaluacionReglaRepository,
                                     EvaluacionResultadoMapper mapper) {
        this.evaluacionRepository = evaluacionRepository;
        this.evaluacionReglaRepository = evaluacionReglaRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResultado> listarTodas() {
        return listar(true);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResultado> listar(boolean soloActivas) {
        if (!soloActivas) {
            return evaluacionRepository.findAll(Sort.by(Direction.DESC, "fechaEvaluacion"))
                    .stream()
                    .map(this::toResultado)
                    .toList();
        }
        return evaluacionRepository.findByEsActivaTrueOrderByFechaEvaluacionDesc()
                .stream()
                .map(this::toResultado)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResultado> listarPorClienteCodigo(String codigoCliente) {
        return evaluacionRepository
                .findByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteOrderByFechaEvaluacionDesc(codigoCliente)
                .stream()
                .map(this::toResultado)
                .toList();
    }

    /**
     * Historial activo filtrado para reportes: por rango de fechas de evaluación,
     * nivel de riesgo y cliente. Un parámetro nulo o vacío no filtra.
     */
    @Transactional(readOnly = true)
    public List<EvaluacionResultado> listarFiltrado(LocalDate desde, LocalDate hasta,
                                                    String nivel, String codigoCliente) {
        return listarTodas().stream()
                .filter(r -> desde == null || !r.fechaEvaluacion().toLocalDate().isBefore(desde))
                .filter(r -> hasta == null || !r.fechaEvaluacion().toLocalDate().isAfter(hasta))
                .filter(r -> nivel == null || nivel.isBlank() || nivel.equals(r.nivel()))
                .filter(r -> codigoCliente == null || codigoCliente.isBlank()
                        || codigoCliente.equals(r.clienteCodigo()))
                .toList();
    }

    private EvaluacionResultado toResultado(EvaluacionFraude evaluacion) {
        List<FactorResultado> factores = evaluacionReglaRepository.findByEvaluacion_IdEvaluacion(evaluacion.getIdEvaluacion())
                .stream()
                .map(mapper::toFactor)
                .toList();
        return mapper.toResultado(evaluacion, factores);
    }
}

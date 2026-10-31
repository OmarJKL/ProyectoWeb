package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.mapper.EvaluacionResultadoMapper;
import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.FactorResultado;
import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.repository.EvaluacionReglaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private EvaluacionResultado toResultado(EvaluacionFraude evaluacion) {
        List<FactorResultado> factores = evaluacionReglaRepository.findByEvaluacion_IdEvaluacion(evaluacion.getIdEvaluacion())
                .stream()
                .map(mapper::toFactor)
                .toList();
        return mapper.toResultado(evaluacion, factores);
    }
}

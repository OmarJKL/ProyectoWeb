package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.KpisResultado;
import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.domain.enums.NivelRiesgo;
import com.lambdashield.fraude.repository.EvaluacionFraudeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Calcula indicadores del historial activo sin mezclar métricas con comandos de transacción. */
@Service
public class EvaluacionMetricasService {

    private final EvaluacionFraudeRepository evaluacionRepository;

    public EvaluacionMetricasService(EvaluacionFraudeRepository evaluacionRepository) {
        this.evaluacionRepository = evaluacionRepository;
    }

    @Transactional(readOnly = true)
    public KpisResultado calcularKpis() {
        List<EvaluacionFraude> evaluaciones = evaluacionRepository.findByEsActivaTrueOrderByFechaEvaluacionDesc();
        long total = evaluaciones.size();
        double promedio = total == 0
                ? 0.0
                : evaluaciones.stream().mapToDouble(e -> e.getFraudeScore().doubleValue()).average().orElse(0.0);
        return new KpisResultado(
                total,
                contar(evaluaciones, NivelRiesgo.BAJO),
                contar(evaluaciones, NivelRiesgo.MEDIO),
                contar(evaluaciones, NivelRiesgo.ALTO),
                contar(evaluaciones, NivelRiesgo.CRITICO),
                promedio
        );
    }

    private static long contar(List<EvaluacionFraude> evaluaciones, NivelRiesgo nivel) {
        return evaluaciones.stream().filter(e -> e.getNivelRiesgo() == nivel).count();
    }
}

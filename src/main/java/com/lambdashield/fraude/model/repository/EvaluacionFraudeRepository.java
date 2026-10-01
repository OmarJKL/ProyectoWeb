package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.EvaluacionFraude;
import com.lambdashield.fraude.model.entity.Transaccion;
import java.util.List;
import java.util.Optional;

// TODO: Extend JpaRepository<EvaluacionFraude, Long>
public interface EvaluacionFraudeRepository {
    List<EvaluacionFraude> findAll();
    Optional<EvaluacionFraude> findByTransaccion(Transaccion transaccion);
    EvaluacionFraude save(EvaluacionFraude evaluacionFraude);
}

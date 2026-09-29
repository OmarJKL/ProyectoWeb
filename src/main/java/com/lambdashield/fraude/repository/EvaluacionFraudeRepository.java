package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.entity.EvaluacionFraude;
import com.lambdashield.fraude.entity.Transaccion;
import java.util.List;
import java.util.Optional;

// TODO: Extend JpaRepository<EvaluacionFraude, Long>
public interface EvaluacionFraudeRepository {
    List<EvaluacionFraude> findAll();
    Optional<EvaluacionFraude> findByTransaccion(Transaccion transaccion);
    EvaluacionFraude save(EvaluacionFraude evaluacionFraude);
}

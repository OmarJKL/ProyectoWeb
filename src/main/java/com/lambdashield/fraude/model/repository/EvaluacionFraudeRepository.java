package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.EvaluacionFraude;
import com.lambdashield.fraude.model.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EvaluacionFraudeRepository extends JpaRepository<EvaluacionFraude, Long> {
    List<EvaluacionFraude> findAll();
    Optional<EvaluacionFraude> findByTransaccion(Transaccion transaccion);
}
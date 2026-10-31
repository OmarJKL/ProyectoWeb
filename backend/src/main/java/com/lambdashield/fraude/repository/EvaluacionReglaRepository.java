package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.EvaluacionRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluacionReglaRepository extends JpaRepository<EvaluacionRegla, Long> {
    List<EvaluacionRegla> findByEvaluacion_IdEvaluacion(Long idEvaluacion);
}

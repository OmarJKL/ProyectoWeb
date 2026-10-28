package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.EvaluacionRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluacionReglaRepository extends JpaRepository<EvaluacionRegla, Long> {
    List<EvaluacionRegla> findByEvaluacion_IdEvaluacion(Long idEvaluacion);
}
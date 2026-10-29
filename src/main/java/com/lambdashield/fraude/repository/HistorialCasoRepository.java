package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.HistorialCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialCasoRepository extends JpaRepository<HistorialCaso, Long> {
    List<HistorialCaso> findByCaso_IdCaso(Long idCaso);
}

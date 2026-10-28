package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.HistorialCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialCasoRepository extends JpaRepository<HistorialCaso, Long> {
    List<HistorialCaso> findByCaso_IdCaso(Long idCaso);
}
package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.AlertaFraude;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, Long> {
    List<AlertaFraude> findByEstadoAlerta(com.lambdashield.fraude.model.enums.EstadoAlerta estado);
    java.util.Optional<AlertaFraude> findByCodigoAlerta(UUID codigoAlerta);
}
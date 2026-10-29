package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.AlertaFraude;
import com.lambdashield.fraude.domain.enums.EstadoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, Long> {
    List<AlertaFraude> findByEstadoAlerta(EstadoAlerta estado);
    Optional<AlertaFraude> findByCodigoAlerta(UUID codigoAlerta);
}

package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.CasoFraude;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CasoFraudeRepository extends JpaRepository<CasoFraude, Long> {
    java.util.Optional<CasoFraude> findByCodigoCaso(UUID codigoCaso);
}

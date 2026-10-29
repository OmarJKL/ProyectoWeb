package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.ReglaFraude;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReglaFraudeRepository extends JpaRepository<ReglaFraude, Integer> {
    List<ReglaFraude> findByEsActivaTrue();
    List<ReglaFraude> findByEsActivaTrueOrderByCodigoReglaAsc();
    Optional<ReglaFraude> findByCodigoRegla(String codigoRegla);
}

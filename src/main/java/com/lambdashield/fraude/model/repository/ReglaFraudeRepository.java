package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.ReglaFraude;
import java.util.List;

// TODO: Extend JpaRepository<ReglaFraude, Integer>
public interface ReglaFraudeRepository {
    List<ReglaFraude> findByEsActivaTrue();
}

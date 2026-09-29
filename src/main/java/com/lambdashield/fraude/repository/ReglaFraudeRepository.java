package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.entity.ReglaFraude;
import java.util.List;

// TODO: Extend JpaRepository<ReglaFraude, Integer>
public interface ReglaFraudeRepository {
    List<ReglaFraude> findByEsActivaTrue();
}

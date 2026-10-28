package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Canal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CanalRepository extends JpaRepository<Canal, Short> {
    Optional<Canal> findByCodigoCanal(String codigoCanal);
}
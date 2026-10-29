package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}

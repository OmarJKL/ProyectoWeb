package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}
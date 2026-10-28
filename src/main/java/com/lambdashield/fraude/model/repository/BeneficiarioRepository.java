package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {
    List<Beneficiario> findByCliente_IdCliente(Long idCliente);
}
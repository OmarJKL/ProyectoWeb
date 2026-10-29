package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {
    List<Beneficiario> findByCliente_IdCliente(Long idCliente);
    Optional<Beneficiario> findFirstByCliente_IdClienteAndNombreBeneficiario(Long idCliente, String nombreBeneficiario);
}

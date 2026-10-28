package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Optional<Cliente> findByCodigoCliente(String codigoCliente);
    List<Cliente> findByEsActivoTrueOrderByNombresAsc();
}
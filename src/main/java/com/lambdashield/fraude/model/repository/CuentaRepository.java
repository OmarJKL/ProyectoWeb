package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    List<Cuenta> findByCliente_IdCliente(Long idCliente);
    Optional<Cuenta> findFirstByCliente_IdClienteOrderByIdCuentaAsc(Long idCliente);
}
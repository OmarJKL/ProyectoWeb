package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.domain.enums.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    List<Cuenta> findByCliente_IdCliente(Long idCliente);
    Optional<Cuenta> findFirstByCliente_IdClienteOrderByIdCuentaAsc(Long idCliente);

    @Query("select coalesce(sum(c.saldoDisponible), 0) from Cuenta c where c.estadoCuenta = :estado")
    BigDecimal sumarSaldoPorEstado(@Param("estado") EstadoCuenta estado);
}

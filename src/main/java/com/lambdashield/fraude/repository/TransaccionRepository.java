package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findAll();
    Optional<Transaccion> findByCodigoTransaccion(UUID codigoTransaccion);
    boolean existsByCuenta_Cliente_CodigoClienteAndDireccionIp(String codigoCliente, String direccionIp);
}

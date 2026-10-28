package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findAll();
    Optional<Transaccion> findByCodigoTransaccion(UUID codigoTransaccion);
}
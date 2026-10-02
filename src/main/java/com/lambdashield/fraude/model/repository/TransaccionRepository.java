package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Transaccion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// TODO: Extend JpaRepository<Transaccion, Long> when DB is connected
public interface TransaccionRepository {
    List<Transaccion> findAll();
    Optional<Transaccion> findByCodigoTransaccion(UUID codigoTransaccion);
    Transaccion save(Transaccion transaccion);
}

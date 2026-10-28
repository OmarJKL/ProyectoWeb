package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Short> {
    Optional<TipoTransaccion> findByCodigoTipo(String codigoTipo);
}
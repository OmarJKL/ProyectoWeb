package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Short> {
    Optional<TipoTransaccion> findByCodigoTipo(String codigoTipo);
}

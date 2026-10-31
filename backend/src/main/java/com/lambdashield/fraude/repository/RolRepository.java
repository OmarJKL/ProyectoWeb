package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Short> {
    Optional<Rol> findByCodigoRol(String codigoRol);
}

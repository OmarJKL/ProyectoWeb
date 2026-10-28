package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Short> {
    Optional<Rol> findByCodigoRol(String codigoRol);
}
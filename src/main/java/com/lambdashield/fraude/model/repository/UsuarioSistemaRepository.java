package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.UsuarioSistema;
import java.util.Optional;

// TODO: Extend JpaRepository<UsuarioSistema, Long>
public interface UsuarioSistemaRepository {
    Optional<UsuarioSistema> findByNombreUsuario(String nombreUsuario);
}

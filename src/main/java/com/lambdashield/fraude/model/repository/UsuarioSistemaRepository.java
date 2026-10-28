package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistema, Long> {
    Optional<UsuarioSistema> findByNombreUsuario(String nombreUsuario);
}
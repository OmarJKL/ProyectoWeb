package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.UsuarioRol;
import com.lambdashield.fraude.domain.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
    List<UsuarioRol> findByUsuario_IdUsuario(Long idUsuario);
}

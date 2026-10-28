package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.UsuarioRol;
import com.lambdashield.fraude.model.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
    List<UsuarioRol> findByUsuario_IdUsuario(Long idUsuario);
}
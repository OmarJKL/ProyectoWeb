package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.UsuarioRol;
import com.lambdashield.fraude.model.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
}
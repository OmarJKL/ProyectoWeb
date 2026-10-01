package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Cliente;
import java.util.List;
import java.util.Optional;

// TODO: Extend JpaRepository<Cliente, Long>
public interface ClienteRepository {
    List<Cliente> findAll();
    Optional<Cliente> findById(Long id);
}

package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.entity.Cliente;
import java.util.List;
import java.util.Optional;

// TODO: Extend JpaRepository<Cliente, Long>
public interface ClienteRepository {
    List<Cliente> findAll();
    Optional<Cliente> findById(Long id);
}

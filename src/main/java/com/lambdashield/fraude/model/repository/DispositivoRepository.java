package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    List<Dispositivo> findByCliente_IdCliente(Long idCliente);
    Optional<Dispositivo> findByHuellaDispositivo(String huella);
}
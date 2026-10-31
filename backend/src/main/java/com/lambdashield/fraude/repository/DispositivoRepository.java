package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    List<Dispositivo> findByCliente_IdCliente(Long idCliente);
    List<Dispositivo> findByCliente_IdClienteAndEsConocidoTrue(Long idCliente);
    Optional<Dispositivo> findByHuellaDispositivo(String huella);
    boolean existsByCliente_IdClienteAndEsConocidoTrueAndDireccionIp(Long idCliente, String direccionIp);
}

package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.device.DeviceFingerprint;
import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Dispositivo;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.DispositivoRepository;
import com.lambdashield.fraude.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta señales del cliente usadas para derivar el contexto de seguridad de una operación. */
@Service
public class ClienteContextService {

    private final ClienteRepository clienteRepository;
    private final DispositivoRepository dispositivoRepository;
    private final TransaccionRepository transaccionRepository;

    public ClienteContextService(ClienteRepository clienteRepository,
                                 DispositivoRepository dispositivoRepository,
                                 TransaccionRepository transaccionRepository) {
        this.clienteRepository = clienteRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional(readOnly = true)
    public boolean esDispositivoConocido(String codigoCliente, String userAgent) {
        if (codigoCliente == null || userAgent == null || userAgent.isBlank()) return false;
        return clienteRepository.findByCodigoCliente(codigoCliente)
                .map(cliente -> {
                    String buscada = DeviceFingerprint.from(codigoCliente, DeviceFingerprint.normalize(userAgent));
                    return dispositivoRepository.findByCliente_IdClienteAndEsConocidoTrue(cliente.getIdCliente())
                            .stream()
                            .map(Dispositivo::getHuellaDispositivo)
                            .anyMatch(buscada::equals);
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean esIpConocida(String codigoCliente, String direccionIp) {
        if (codigoCliente == null || direccionIp == null || direccionIp.isBlank()) return false;
        Cliente cliente = clienteRepository.findByCodigoCliente(codigoCliente).orElse(null);
        if (cliente == null) return false;
        return dispositivoRepository.existsByCliente_IdClienteAndEsConocidoTrueAndDireccionIp(
                cliente.getIdCliente(), direccionIp)
                || transaccionRepository.existsByCuenta_Cliente_CodigoClienteAndDireccionIp(
                codigoCliente, direccionIp);
    }
}

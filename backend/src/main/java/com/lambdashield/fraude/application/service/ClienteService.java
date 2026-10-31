package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.domain.customer.NombreClienteFormatter;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.CuentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a los datos de clientes que necesita la aplicación.
 *
 * Toda la información se consulta desde PostgreSQL: no queda ningún catálogo
 * de clientes en memoria.
 */
@Service
public class ClienteService {

    public record ClienteInfo(
            String codigo,
            String nombre,
            double promedioGasto,
            String numeroCuenta,
            double saldo
    ) {}

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          CuentaRepository cuentaRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarEntidad(String codigoCliente) {
        return clienteRepository.findByCodigoCliente(codigoCliente);
    }

    @Transactional(readOnly = true)
    public Optional<ClienteInfo> buscar(String codigoCliente) {
        return buscarEntidad(codigoCliente).map(this::toInfo);
    }

    @Transactional(readOnly = true)
    public List<ClienteInfo> listarTodos() {
        return clienteRepository.findByEsActivoTrueOrderByNombresAsc()
                .stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaPrincipal(Cliente cliente) {
        return cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente())
                .orElseThrow(() -> new IllegalStateException(
                        "El cliente " + cliente.getCodigoCliente() + " no tiene una cuenta registrada"));
    }

    private ClienteInfo toInfo(Cliente cliente) {
        Cuenta cuenta = cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente())
                .orElse(null);
        BigDecimal promedio = cliente.getPromedioGasto() != null
                ? cliente.getPromedioGasto()
                : BigDecimal.ZERO;
        return new ClienteInfo(
                cliente.getCodigoCliente(),
                NombreClienteFormatter.nombreCompleto(cliente),
                promedio.doubleValue(),
                cuenta != null ? cuenta.getNumeroCuenta() : "",
                cuenta != null && cuenta.getSaldoDisponible() != null
                        ? cuenta.getSaldoDisponible().doubleValue()
                        : 0.0
        );
    }

}

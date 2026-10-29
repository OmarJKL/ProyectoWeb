package com.lambdashield.fraude.service;

import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.domain.entity.Dispositivo;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.CuentaRepository;
import com.lambdashield.fraude.repository.DispositivoRepository;
import com.lambdashield.fraude.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
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
    private final DispositivoRepository dispositivoRepository;
    private final TransaccionRepository transaccionRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          CuentaRepository cuentaRepository,
                          DispositivoRepository dispositivoRepository,
                          TransaccionRepository transaccionRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.transaccionRepository = transaccionRepository;
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
    public boolean esDispositivoConocido(String codigoCliente, String userAgent) {
        if (codigoCliente == null || userAgent == null || userAgent.isBlank()) {
            return false;
        }

        Optional<Cliente> cliente = clienteRepository.findByCodigoCliente(codigoCliente);
        if (cliente.isEmpty()) {
            return false;
        }

        String huellaBuscada = huella(codigoCliente, normalizarDispositivo(userAgent));
        return dispositivoRepository.findByCliente_IdClienteAndEsConocidoTrue(cliente.get().getIdCliente())
                .stream()
                .map(Dispositivo::getHuellaDispositivo)
                .anyMatch(huellaBuscada::equals);
    }

    @Transactional(readOnly = true)
    public boolean esIpConocida(String codigoCliente, String direccionIp) {
        if (codigoCliente == null || direccionIp == null || direccionIp.isBlank()) {
            return false;
        }

        Optional<Cliente> cliente = clienteRepository.findByCodigoCliente(codigoCliente);
        if (cliente.isEmpty()) {
            return false;
        }

        if (dispositivoRepository.existsByCliente_IdClienteAndEsConocidoTrueAndDireccionIp(
                cliente.get().getIdCliente(), direccionIp)) {
            return true;
        }
        return transaccionRepository.existsByCuenta_Cliente_CodigoClienteAndDireccionIp(
                codigoCliente, direccionIp);
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaPrincipal(Cliente cliente) {
        return cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente())
                .orElseThrow(() -> new IllegalStateException(
                        "El cliente " + cliente.getCodigoCliente() + " no tiene una cuenta registrada"));
    }

    /**
     * Normaliza el User-Agent a una huella estable para compararla con la base
     * de dispositivos conocidos.
     */
    public static String normalizarDispositivo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Desconocido|Desconocido";
        }

        String ua = userAgent.toLowerCase(Locale.ROOT);
        String navegador = ua.contains("edg/") ? "Edge"
                : ua.contains("firefox/") ? "Firefox"
                : ua.contains("chrome/") || ua.contains("crios/") ? "Chrome"
                : ua.contains("safari/") ? "Safari"
                : "Otro";

        String sistema = ua.contains("android") ? "Android"
                : ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios") ? "iOS"
                : ua.contains("windows") ? "Windows"
                : ua.contains("mac os") || ua.contains("macintosh") ? "macOS"
                : ua.contains("linux") ? "Linux"
                : "Otro";

        return navegador + "|" + sistema;
    }

    public static String huella(String codigoCliente, String dispositivoNormalizado) {
        return codigoCliente + "|" + dispositivoNormalizado;
    }

    private ClienteInfo toInfo(Cliente cliente) {
        Cuenta cuenta = cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente())
                .orElse(null);
        BigDecimal promedio = cliente.getPromedioGasto() != null
                ? cliente.getPromedioGasto()
                : BigDecimal.ZERO;
        return new ClienteInfo(
                cliente.getCodigoCliente(),
                nombreCompleto(cliente),
                promedio.doubleValue(),
                cuenta != null ? cuenta.getNumeroCuenta() : "",
                cuenta != null && cuenta.getSaldoDisponible() != null
                        ? cuenta.getSaldoDisponible().doubleValue()
                        : 0.0
        );
    }

    public static String nombreCompleto(Cliente cliente) {
        StringBuilder nombre = new StringBuilder();
        if (cliente.getNombres() != null && !cliente.getNombres().isBlank()) {
            nombre.append(cliente.getNombres().trim());
        }
        if (cliente.getApellidoPaterno() != null && !cliente.getApellidoPaterno().isBlank()) {
            if (!nombre.isEmpty()) nombre.append(' ');
            nombre.append(cliente.getApellidoPaterno().trim());
        }
        if (cliente.getApellidoMaterno() != null && !cliente.getApellidoMaterno().isBlank()) {
            if (!nombre.isEmpty()) nombre.append(' ');
            nombre.append(cliente.getApellidoMaterno().trim());
        }
        return nombre.isEmpty() ? cliente.getCodigoCliente() : nombre.toString();
    }
}

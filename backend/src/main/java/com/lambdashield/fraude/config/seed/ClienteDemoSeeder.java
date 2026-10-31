package com.lambdashield.fraude.config.seed;

import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.domain.entity.Dispositivo;
import com.lambdashield.fraude.domain.device.DeviceFingerprint;
import com.lambdashield.fraude.domain.enums.EstadoCuenta;
import com.lambdashield.fraude.domain.enums.TipoCuenta;
import com.lambdashield.fraude.domain.enums.TipoDispositivo;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.CuentaRepository;
import com.lambdashield.fraude.repository.DispositivoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Semillas idempotentes para clientes y sus datos bancarios de demostración. */
@Component
public class ClienteDemoSeeder {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final DispositivoRepository dispositivoRepository;

    public ClienteDemoSeeder(ClienteRepository clienteRepository,
                             CuentaRepository cuentaRepository,
                             DispositivoRepository dispositivoRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.dispositivoRepository = dispositivoRepository;
    }

    public void seed() {
        crearCliente("C-001", "DNI", "10000001", "María", "Fernández", "Rojas",
                "maria.fernandez@lambda.local", "999111222", new BigDecimal("180.00"));
        crearCliente("C-002", "DNI", "10000002", "Jorge", "Salas", "Mendoza",
                "jorge.salas@lambda.local", "999333444", new BigDecimal("950.00"));
        crearCliente("C-003", "DNI", "10000003", "Ana", "Torres", "Vega",
                "ana.torres@lambda.local", "999555666", new BigDecimal("420.00"));

        Cliente c1 = clienteRepository.findByCodigoCliente("C-001").orElseThrow();
        Cliente c2 = clienteRepository.findByCodigoCliente("C-002").orElseThrow();
        Cliente c3 = clienteRepository.findByCodigoCliente("C-003").orElseThrow();
        crearCuenta(c1, "0011-0145-0200847291", new BigDecimal("4850.00"));
        crearCuenta(c2, "0011-0145-0200847292", new BigDecimal("12500.00"));
        crearCuenta(c3, "0011-0145-0200847293", new BigDecimal("780.00"));
        crearDispositivos(c1, List.of("Chrome|Windows", "Edge|Windows"));
        crearDispositivos(c2, List.of("Chrome|Android", "Chrome|Windows"));
        crearDispositivos(c3, List.of("Firefox|Windows", "Safari|macOS"));
    }

    private void crearCliente(String codigo, String tipoDocumento, String numeroDocumento,
                              String nombres, String apellidoPaterno, String apellidoMaterno,
                              String correo, String telefono, BigDecimal promedioGasto) {
        if (clienteRepository.findByCodigoCliente(codigo).isPresent()) return;
        clienteRepository.save(Cliente.builder()
                .codigoCliente(codigo)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .nombres(nombres)
                .apellidoPaterno(apellidoPaterno)
                .apellidoMaterno(apellidoMaterno)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .correoElectronico(correo)
                .numeroTelefono(telefono)
                .promedioGasto(promedioGasto)
                .esActivo(true)
                .build());
    }

    private void crearCuenta(Cliente cliente, String numeroCuenta, BigDecimal saldo) {
        if (cuentaRepository.findFirstByCliente_IdClienteOrderByIdCuentaAsc(cliente.getIdCliente()).isPresent()) return;
        cuentaRepository.save(Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .tipoCuenta(TipoCuenta.AHORROS)
                .codigoMoneda("PEN")
                .saldoDisponible(saldo)
                .estadoCuenta(EstadoCuenta.ACTIVA)
                .fechaApertura(LocalDate.now().minusYears(2))
                .fechaCreacion(OffsetDateTime.now())
                .build());
    }

    private void crearDispositivos(Cliente cliente, List<String> dispositivos) {
        if (!dispositivoRepository.findByCliente_IdCliente(cliente.getIdCliente()).isEmpty()) return;
        for (String normalizado : dispositivos) {
            String[] partes = normalizado.split("\\|", 2);
            String navegador = partes[0];
            String sistema = partes[1];
            dispositivoRepository.save(Dispositivo.builder()
                    .cliente(cliente)
                    .huellaDispositivo(DeviceFingerprint.from(cliente.getCodigoCliente(), normalizado))
                    .tipoDispositivo("Android".equals(sistema) || "iOS".equals(sistema)
                            ? TipoDispositivo.MOVIL : TipoDispositivo.PC)
                    .sistemaOperativo(sistema)
                    .navegador(navegador)
                    .direccionIp("127.0.0.1")
                    .esConocido(true)
                    .fechaPrimerUso(OffsetDateTime.now().minusMonths(6))
                    .fechaUltimoUso(OffsetDateTime.now())
                    .build());
        }
    }
}

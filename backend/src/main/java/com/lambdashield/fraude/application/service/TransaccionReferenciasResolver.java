package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.entity.Beneficiario;
import com.lambdashield.fraude.domain.entity.Canal;
import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.Dispositivo;
import com.lambdashield.fraude.domain.entity.TipoTransaccion;
import com.lambdashield.fraude.domain.device.DeviceFingerprint;
import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.domain.enums.TipoDispositivo;
import com.lambdashield.fraude.application.command.EvaluarTransaccionCommand;
import com.lambdashield.fraude.repository.BeneficiarioRepository;
import com.lambdashield.fraude.repository.CanalRepository;
import com.lambdashield.fraude.repository.DispositivoRepository;
import com.lambdashield.fraude.repository.TipoTransaccionRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/** Resuelve y crea las entidades de catálogo asociadas a una transacción. */
@Component
public class TransaccionReferenciasResolver {

    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final CanalRepository canalRepository;
    private final DispositivoRepository dispositivoRepository;
    private final BeneficiarioRepository beneficiarioRepository;

    public TransaccionReferenciasResolver(TipoTransaccionRepository tipoTransaccionRepository,
                                          CanalRepository canalRepository,
                                          DispositivoRepository dispositivoRepository,
                                          BeneficiarioRepository beneficiarioRepository) {
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.canalRepository = canalRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
    }

    public Referencias resolver(Cliente cliente, EvaluarTransaccionCommand request, String userAgent) {
        return new Referencias(
                tipoTransaccion(request.categoria()),
                canalWeb(),
                dispositivo(cliente, request.dispositivo(), userAgent),
                beneficiario(cliente, request.beneficiario(), request.beneficiarioNombre())
        );
    }

    private TipoTransaccion tipoTransaccion(String categoria) {
        String codigo = categoria == null || categoria.isBlank()
                ? "otros"
                : categoria.trim().toLowerCase(Locale.ROOT);
        return tipoTransaccionRepository.findByCodigoTipo(codigo)
                .orElseGet(() -> tipoTransaccionRepository.save(TipoTransaccion.builder()
                        .codigoTipo(codigo)
                        .nombreTipo(capitalizar(codigo))
                        .naturaleza(NaturalezaTransaccion.DEBITO)
                        .esActivo(true)
                        .build()));
    }

    private Canal canalWeb() {
        return canalRepository.findByCodigoCanal("WEB")
                .orElseGet(() -> canalRepository.save(Canal.builder()
                        .codigoCanal("WEB")
                        .nombreCanal("Banca por internet")
                        .esActivo(true)
                        .build()));
    }

    private Dispositivo dispositivo(Cliente cliente, String tipoSolicitado, String userAgent) {
        if ("reconocido".equalsIgnoreCase(tipoSolicitado)) {
            return dispositivoRepository.findByCliente_IdClienteAndEsConocidoTrue(cliente.getIdCliente())
                    .stream()
                    .findFirst()
                    .orElseGet(() -> crearDispositivo(cliente,
                            DeviceFingerprint.normalize(userAgent), true));
        }
        String normalizado = userAgent != null
                ? DeviceFingerprint.normalize(userAgent)
                : "Nuevo|Desconocido";
        return crearDispositivo(cliente, normalizado, false);
    }

    private Dispositivo crearDispositivo(Cliente cliente, String normalizado, boolean conocido) {
        String[] partes = normalizado.split("\\|", 2);
        String navegador = partes.length > 0 ? partes[0] : "Otro";
        String sistema = partes.length > 1 ? partes[1] : "Otro";
        TipoDispositivo tipo = "Android".equals(sistema) || "iOS".equals(sistema)
                ? TipoDispositivo.MOVIL
                : TipoDispositivo.PC;
        OffsetDateTime ahora = OffsetDateTime.now();
        return dispositivoRepository.save(Dispositivo.builder()
                .cliente(cliente)
                .huellaDispositivo(DeviceFingerprint.from(cliente.getCodigoCliente(), normalizado)
                        + "|" + UUID.randomUUID())
                .tipoDispositivo(tipo)
                .sistemaOperativo(sistema)
                .navegador(navegador)
                .esConocido(conocido)
                .fechaPrimerUso(ahora)
                .fechaUltimoUso(ahora)
                .build());
    }

    /**
     * Resuelve el beneficiario de la operación. Si se conoce su nombre, el tipo
     * (nuevo o recurrente) se deriva del historial real del cliente: la primera
     * vez se crea como nuevo y en las siguientes transferencias al mismo
     * destinatario pasa a recurrente. Sin nombre, se respeta el tipo declarado
     * con un destinatario genérico (simulador del administrador).
     */
    private Beneficiario beneficiario(Cliente cliente, String tipoBeneficiario, String nombreBeneficiario) {
        if (nombreBeneficiario != null && !nombreBeneficiario.isBlank()) {
            String nombre = nombreBeneficiario.trim();
            return beneficiarioRepository
                    .findFirstByCliente_IdClienteAndNombreBeneficiario(cliente.getIdCliente(), nombre)
                    .map(existente -> {
                        existente.setEsConocido(true);
                        existente.setFechaUltimoUso(OffsetDateTime.now());
                        return beneficiarioRepository.save(existente);
                    })
                    .orElseGet(() -> beneficiarioRepository.save(Beneficiario.builder()
                            .cliente(cliente)
                            .nombreBeneficiario(nombre)
                            .entidadFinanciera("Lambda Bank")
                            .codigoPais("PE")
                            .esConocido(false)
                            .fechaPrimerUso(OffsetDateTime.now())
                            .fechaUltimoUso(OffsetDateTime.now())
                            .esActivo(true)
                            .build()));
        }

        boolean nuevo = "nuevo".equalsIgnoreCase(tipoBeneficiario);
        String nombre = nuevo ? "Destinatario nuevo" : "Destinatario recurrente";
        return beneficiarioRepository
                .findFirstByCliente_IdClienteAndNombreBeneficiario(cliente.getIdCliente(), nombre)
                .orElseGet(() -> beneficiarioRepository.save(Beneficiario.builder()
                        .cliente(cliente)
                        .nombreBeneficiario(nombre)
                        .entidadFinanciera("Lambda Bank")
                        .codigoPais("PE")
                        .esConocido(!nuevo)
                        .fechaPrimerUso(OffsetDateTime.now())
                        .fechaUltimoUso(OffsetDateTime.now())
                        .esActivo(true)
                        .build()));
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    public record Referencias(TipoTransaccion tipoTransaccion, Canal canal,
                              Dispositivo dispositivo, Beneficiario beneficiario) {}
}

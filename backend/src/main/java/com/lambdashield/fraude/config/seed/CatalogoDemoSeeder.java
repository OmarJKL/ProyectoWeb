package com.lambdashield.fraude.config.seed;

import com.lambdashield.fraude.domain.entity.Canal;
import com.lambdashield.fraude.domain.entity.ReglaFraude;
import com.lambdashield.fraude.domain.entity.Rol;
import com.lambdashield.fraude.domain.entity.TipoTransaccion;
import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.domain.enums.NivelRiesgo;
import com.lambdashield.fraude.repository.CanalRepository;
import com.lambdashield.fraude.repository.ReglaFraudeRepository;
import com.lambdashield.fraude.repository.RolRepository;
import com.lambdashield.fraude.repository.TipoTransaccionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Semillas idempotentes para los catálogos que requiere la aplicación. */
@Component
public class CatalogoDemoSeeder {

    private record ReglaSeed(String codigo, String nombre, String descripcion, int peso) {}

    private static final List<ReglaSeed> REGLAS = List.of(
            new ReglaSeed("RF001", "Monto inusual", "Puntúa gradualmente cuando el monto supera el promedio histórico del cliente.", 25),
            new ReglaSeed("RF002", "Dispositivo nuevo", "Puntúa cuando la operación se realiza desde un dispositivo no registrado.", 15),
            new ReglaSeed("RF003", "IP nueva", "Puntúa cuando la operación proviene de una dirección IP no habitual.", 10),
            new ReglaSeed("RF004", "País nuevo", "Puntúa operaciones originadas en países o jurisdicciones poco frecuentes.", 30),
            new ReglaSeed("RF005", "Horario inusual", "Puntúa operaciones realizadas en la madrugada (00:00-04:59).", 10),
            new ReglaSeed("RF006", "Alta frecuencia", "Puntúa ráfagas de operaciones en la última hora, típicas de pruebas de tarjeta.", 25),
            new ReglaSeed("RF007", "Viaje imposible", "Heurística simplificada basada en país, frecuencia e IP.", 40),
            new ReglaSeed("RF008", "Beneficiario nuevo con monto alto", "Puntúa cuando el beneficiario es nuevo y el monto supera el umbral.", 20)
    );

    private final RolRepository rolRepository;
    private final CanalRepository canalRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final ReglaFraudeRepository reglaFraudeRepository;

    public CatalogoDemoSeeder(RolRepository rolRepository,
                              CanalRepository canalRepository,
                              TipoTransaccionRepository tipoTransaccionRepository,
                              ReglaFraudeRepository reglaFraudeRepository) {
        this.rolRepository = rolRepository;
        this.canalRepository = canalRepository;
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.reglaFraudeRepository = reglaFraudeRepository;
    }

    public void seedRoles() {
        crearRol("ADMIN", "Administrador", "Acceso total al sistema");
        crearRol("ANALISTA", "Analista antifraude", "Consulta y monitoreo de operaciones");
        crearRol("CLIENTE", "Cliente", "Portal de banca en línea");
    }

    public void seedCatalogos() {
        canalRepository.findByCodigoCanal("WEB").orElseGet(() -> canalRepository.save(Canal.builder()
                .codigoCanal("WEB")
                .nombreCanal("Banca por internet")
                .esActivo(true)
                .build()));

        for (String categoria : List.of("servicios", "retail", "electronica", "restaurante",
                "cripto", "casino", "otros")) {
            tipoTransaccionRepository.findByCodigoTipo(categoria).orElseGet(() ->
                    tipoTransaccionRepository.save(TipoTransaccion.builder()
                            .codigoTipo(categoria)
                            .nombreTipo(capitalizar(categoria))
                            .naturaleza(NaturalezaTransaccion.DEBITO)
                            .esActivo(true)
                            .build()));
        }
    }

    public void seedReglas() {
        for (ReglaSeed seed : REGLAS) {
            if (reglaFraudeRepository.findByCodigoRegla(seed.codigo()).isPresent()) continue;
            reglaFraudeRepository.save(ReglaFraude.builder()
                    .codigoRegla(seed.codigo())
                    .nombreRegla(seed.nombre())
                    .descripcion(seed.descripcion())
                    .nivelRiesgo(NivelRiesgo.ALTO)
                    .peso(BigDecimal.valueOf(seed.peso()))
                    .versionRegla(1)
                    .esActiva(true)
                    .fechaInicioVigencia(OffsetDateTime.now())
                    .build());
        }
    }

    private void crearRol(String codigo, String nombre, String descripcion) {
        rolRepository.findByCodigoRol(codigo).orElseGet(() -> rolRepository.save(Rol.builder()
                .codigoRol(codigo)
                .nombreRol(nombre)
                .descripcion(descripcion)
                .esActivo(true)
                .build()));
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}

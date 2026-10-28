package com.lambdashield.fraude.model.rule;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de valores límite de las ocho reglas del motor antifraude.
 */
class ReglasFraudeTest {

    private static TransaccionCrearRequest transaccion(double monto, int hora, String pais,
                                                       String dispositivo, int operaciones,
                                                       String beneficiario, String ip) {
        return new TransaccionCrearRequest(
                "C-001", monto, "retail", hora, pais, dispositivo, operaciones, beneficiario, ip
        );
    }

    private static TransaccionCrearRequest base(double monto) {
        return transaccion(monto, 12, "local", "reconocido", 1, "recurrente", "conocida");
    }

    @Test
    void rf001_montoInusual_aplicaEscalaGradual() {
        ReglaMontoInusual regla = new ReglaMontoInusual();

        // Promedio 100: proporción = monto / 100
        assertThat(regla.evaluar(base(150.0), 100.0).puntaje()).isZero();       // 1.5x
        assertThat(regla.evaluar(base(225.0), 100.0).puntaje()).isEqualTo(10);  // 2.25x
        assertThat(regla.evaluar(base(300.0), 100.0).puntaje()).isEqualTo(15);  // 3.0x
        assertThat(regla.evaluar(base(400.0), 100.0).puntaje()).isEqualTo(19);  // 4.0x
        assertThat(regla.evaluar(base(500.0), 100.0).puntaje()).isEqualTo(25);  // 5.0x
        assertThat(regla.evaluar(base(500.0), 100.0).max()).isEqualTo(25);
    }

    @Test
    void rf002_dispositivoNuevo_soloPuntuaSiEsNuevo() {
        ReglaDispositivo regla = new ReglaDispositivo();

        assertThat(regla.evaluar(
                transaccion(100, 12, "local", "nuevo", 1, "recurrente", "conocida"), 100).puntaje())
                .isEqualTo(15);
        assertThat(regla.evaluar(
                transaccion(100, 12, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje())
                .isZero();
    }

    @Test
    void rf003_ipNueva_soloPuntuaSiEsNueva() {
        ReglaIp regla = new ReglaIp();

        assertThat(regla.evaluar(
                transaccion(100, 12, "local", "reconocido", 1, "recurrente", "nueva"), 100).puntaje())
                .isEqualTo(10);
        assertThat(regla.evaluar(
                transaccion(100, 12, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje())
                .isZero();
    }

    @Test
    void rf004_paisNuevo_puntuaRegionalYAltoRiesgo() {
        ReglaUbicacion regla = new ReglaUbicacion();

        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 12, "regional", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isEqualTo(15);
        assertThat(regla.evaluar(transaccion(100, 12, "alto_riesgo", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isEqualTo(30);
    }

    @Test
    void rf005_horarioInusual_soloPuntuaDeMadrugada() {
        ReglaHorario regla = new ReglaHorario();

        assertThat(regla.evaluar(transaccion(100, 0, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isEqualTo(10);
        assertThat(regla.evaluar(transaccion(100, 4, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isEqualTo(10);
        assertThat(regla.evaluar(transaccion(100, 5, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 22, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 23, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
    }

    @Test
    void rf006_altaFrecuencia_puntuaSegunCantidadDeOperaciones() {
        ReglaVelocidad regla = new ReglaVelocidad();

        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 2, "recurrente", "conocida"), 100).puntaje()).isEqualTo(5);
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 3, "recurrente", "conocida"), 100).puntaje()).isEqualTo(12);
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 4, "recurrente", "conocida"), 100).puntaje()).isEqualTo(15);
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 5, "recurrente", "conocida"), 100).puntaje()).isEqualTo(20);
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 6, "recurrente", "conocida"), 100).puntaje()).isEqualTo(25);
    }

    @Test
    void rf007_viajeImposible_usaHeuristicaPaisFrecuenciaEIp() {
        ReglaViajeImposible regla = new ReglaViajeImposible();

        assertThat(regla.evaluar(transaccion(100, 12, "regional", "reconocido", 2, "recurrente", "conocida"), 100).puntaje()).isEqualTo(40);
        assertThat(regla.evaluar(transaccion(100, 12, "alto_riesgo", "reconocido", 2, "recurrente", "conocida"), 100).puntaje()).isEqualTo(40);
        assertThat(regla.evaluar(transaccion(100, 12, "alto_riesgo", "reconocido", 1, "recurrente", "nueva"), 100).puntaje()).isEqualTo(28);
        assertThat(regla.evaluar(transaccion(100, 12, "alto_riesgo", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 12, "regional", "reconocido", 1, "recurrente", "nueva"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(100, 12, "local", "reconocido", 6, "recurrente", "nueva"), 100).puntaje()).isZero();

        // La categoría casino ya no activa la regla de viaje imposible.
        TransaccionCrearRequest casinoLocal = new TransaccionCrearRequest(
                "C-001", 100, "casino", 12, "local", "reconocido", 6, "recurrente", "nueva"
        );
        assertThat(regla.evaluar(casinoLocal, 100).puntaje()).isZero();
    }

    @Test
    void rf008_beneficiarioNuevo_soloPuntuaConMontoSobreUmbral() {
        ReglaBeneficiario regla = new ReglaBeneficiario();

        // Promedio 100 => umbral 150
        assertThat(regla.evaluar(transaccion(151, 12, "local", "reconocido", 1, "nuevo", "conocida"), 100).puntaje()).isEqualTo(20);
        assertThat(regla.evaluar(transaccion(150, 12, "local", "reconocido", 1, "nuevo", "conocida"), 100).puntaje()).isZero();
        assertThat(regla.evaluar(transaccion(500, 12, "local", "reconocido", 1, "recurrente", "conocida"), 100).puntaje()).isZero();

        // Sin promedio histórico el umbral por defecto es 500.
        assertThat(regla.evaluar(transaccion(501, 12, "local", "reconocido", 1, "nuevo", "conocida"), 0).puntaje()).isEqualTo(20);
        assertThat(regla.evaluar(transaccion(500, 12, "local", "reconocido", 1, "nuevo", "conocida"), 0).puntaje()).isZero();
    }
}

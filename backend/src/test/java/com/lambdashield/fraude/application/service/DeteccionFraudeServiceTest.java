package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.domain.fraud.FactorEvaluado;
import com.lambdashield.fraude.domain.fraud.TransaccionEvaluable;
import com.lambdashield.fraude.domain.fraud.rule.ReglaBeneficiario;
import com.lambdashield.fraude.domain.fraud.rule.ReglaDispositivo;
import com.lambdashield.fraude.domain.fraud.rule.ReglaHorario;
import com.lambdashield.fraude.domain.fraud.rule.ReglaIp;
import com.lambdashield.fraude.domain.fraud.rule.ReglaMontoInusual;
import com.lambdashield.fraude.domain.fraud.rule.ReglaUbicacion;
import com.lambdashield.fraude.domain.fraud.rule.ReglaVelocidad;
import com.lambdashield.fraude.domain.fraud.rule.ReglaViajeImposible;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeteccionFraudeServiceTest {

    private final DeteccionFraudeService service = new DeteccionFraudeService(List.of());

    @Test
    void clasificaNivelEnLosUmbralesExactos() {
        assertThat(service.clasificarNivel(0)).isEqualTo("bajo");
        assertThat(service.clasificarNivel(29)).isEqualTo("bajo");
        assertThat(service.clasificarNivel(30)).isEqualTo("medio");
        assertThat(service.clasificarNivel(59)).isEqualTo("medio");
        assertThat(service.clasificarNivel(60)).isEqualTo("alto");
        assertThat(service.clasificarNivel(79)).isEqualTo("alto");
        assertThat(service.clasificarNivel(80)).isEqualTo("critico");
        assertThat(service.clasificarNivel(100)).isEqualTo("critico");
    }

    @Test
    void aplicaTopeDeCienAlPuntajeTotal() {
        List<FactorEvaluado> factores = List.of(
                new FactorEvaluado("monto", "RF001", 60, 100, "prueba"),
                new FactorEvaluado("viaje", "RF007", 60, 100, "prueba")
        );

        assertThat(service.calcularPuntajeTotal(factores)).isEqualTo(100);
    }

    @Test
    void sumaElPuntajeDeLosFactoresSinSuperarCien() {
        List<FactorEvaluado> factores = List.of(
                new FactorEvaluado("monto", "RF001", 25, 25, "prueba"),
                new FactorEvaluado("horario", "RF005", 10, 10, "prueba")
        );

        assertThat(service.calcularPuntajeTotal(factores)).isEqualTo(35);
        assertThat(service.calcularPuntajeTotal(List.of())).isZero();
    }

    @Test
    void evaluaConLasOchoReglasRegistradas() {
        DeteccionFraudeService completo = new DeteccionFraudeService(List.of(
                new ReglaMontoInusual(),
                new ReglaDispositivo(),
                new ReglaIp(),
                new ReglaUbicacion(),
                new ReglaHorario(),
                new ReglaVelocidad(),
                new ReglaViajeImposible(),
                new ReglaBeneficiario()
        ));
        TransaccionEvaluable request = new TransaccionEvaluable(
                1000.0, 12, "local", "reconocido", 1, "recurrente", "conocida"
        );

        List<FactorEvaluado> factores = completo.evaluar(request, 100.0);

        assertThat(factores).hasSize(8);
        assertThat(factores).extracting(FactorEvaluado::id)
                .containsExactlyInAnyOrder("monto", "dispositivo", "ip", "ubicacion",
                        "horario", "velocidad", "viaje_imposible", "beneficiario");
        assertThat(completo.calcularPuntajeTotal(factores)).isBetween(0, 100);
    }
}

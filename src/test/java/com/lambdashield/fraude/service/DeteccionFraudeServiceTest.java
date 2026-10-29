package com.lambdashield.fraude.service;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.FactorResponse;
import com.lambdashield.fraude.service.rule.ReglaBeneficiario;
import com.lambdashield.fraude.service.rule.ReglaDispositivo;
import com.lambdashield.fraude.service.rule.ReglaHorario;
import com.lambdashield.fraude.service.rule.ReglaIp;
import com.lambdashield.fraude.service.rule.ReglaMontoInusual;
import com.lambdashield.fraude.service.rule.ReglaUbicacion;
import com.lambdashield.fraude.service.rule.ReglaVelocidad;
import com.lambdashield.fraude.service.rule.ReglaViajeImposible;
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
        List<FactorResponse> factores = List.of(
                new FactorResponse("monto", "RF001", 60, 100, "prueba"),
                new FactorResponse("viaje", "RF007", 60, 100, "prueba")
        );

        assertThat(service.calcularPuntajeTotal(factores)).isEqualTo(100);
    }

    @Test
    void sumaElPuntajeDeLosFactoresSinSuperarCien() {
        List<FactorResponse> factores = List.of(
                new FactorResponse("monto", "RF001", 25, 25, "prueba"),
                new FactorResponse("horario", "RF005", 10, 10, "prueba")
        );

        assertThat(service.calcularPuntajeTotal(factores)).isEqualTo(35);
        assertThat(service.calcularPuntajeTotal(List.of())).isZero();
    }

    @Test
    void determinaElEstadoDeCadaNivel() {
        assertThat(service.determinarEstado("bajo")).isEqualTo("Aprobada");
        assertThat(service.determinarEstado("medio")).isEqualTo("Monitoreo");
        assertThat(service.determinarEstado("alto")).isEqualTo("Retener / Alertar");
        assertThat(service.determinarEstado("critico")).isEqualTo("Bloqueada");
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
        TransaccionCrearRequest request = new TransaccionCrearRequest(
                "C-001", 1000.0, "retail", 12, "local",
                "reconocido", 1, "recurrente", "conocida"
        );

        List<FactorResponse> factores = completo.evaluar(request, 100.0);

        assertThat(factores).hasSize(8);
        assertThat(factores).extracting(FactorResponse::id)
                .containsExactlyInAnyOrder("monto", "dispositivo", "ip", "ubicacion",
                        "horario", "velocidad", "viaje_imposible", "beneficiario");
        assertThat(completo.calcularPuntajeTotal(factores)).isBetween(0, 100);
    }
}

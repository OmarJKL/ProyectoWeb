package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.domain.entity.Beneficiario;
import com.lambdashield.fraude.domain.entity.Cuenta;
import com.lambdashield.fraude.repository.BeneficiarioRepository;
import com.lambdashield.fraude.repository.ClienteRepository;
import com.lambdashield.fraude.repository.CuentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comportamiento del portal del cliente: el tipo de destinatario se deriva del
 * historial real y el saldo se descuenta de la cuenta al aprobarse una
 * operación (y se rechaza si los fondos no alcanzan).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortalClienteApiMockMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    private static MockHttpSession sesion(String rol, String codigoCliente) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", "prueba-" + rol.toLowerCase());
        session.setAttribute("nombreUsuario", "Usuario " + rol);
        session.setAttribute("rol", rol);
        if (codigoCliente != null) {
            session.setAttribute("codigoCliente", codigoCliente);
        }
        return session;
    }

    private static String json(String cliente, double monto, String beneficiarioTipo, String beneficiarioNombre) {
        return """
                {
                  "cliente": "%s",
                  "monto": %s,
                  "categoria": "retail",
                  "hora": 12,
                  "pais": "local",
                  "dispositivo": "reconocido",
                  "operacionesUltimaHora": 0,
                  "beneficiario": "%s",
                  "ip": "conocida",
                  "beneficiarioNombre": "%s"
                }
                """.formatted(cliente, monto, beneficiarioTipo, beneficiarioNombre);
    }

    @Test
    void tipoDeBeneficiarioSeDerivaDelHistorial() throws Exception {
        Long idCliente = clienteRepository.findByCodigoCliente("C-001").orElseThrow().getIdCliente();

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(sesion("CLIENTE", "C-001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("C-001", 20, "", "Carlos Mendoza")))
                .andExpect(status().isOk());

        Beneficiario primero = beneficiarioRepository
                .findFirstByCliente_IdClienteAndNombreBeneficiario(idCliente, "Carlos Mendoza").orElseThrow();
        assertThat(primero.isEsConocido())
                .as("la primera transferencia a un destinatario lo marca como nuevo")
                .isFalse();

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(sesion("CLIENTE", "C-001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("C-001", 20, "", "Carlos Mendoza")))
                .andExpect(status().isOk());

        Beneficiario segundo = beneficiarioRepository
                .findFirstByCliente_IdClienteAndNombreBeneficiario(idCliente, "Carlos Mendoza").orElseThrow();
        assertThat(segundo.isEsConocido())
                .as("al repetir transferencias al mismo destinatario pasa a frecuente")
                .isTrue();
    }

    @Test
    void operacionAprobadaDescuentaElSaldoDeLaCuenta() throws Exception {
        BigDecimal saldoAntes = saldoDe("C-001");

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(sesion("ADMIN", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("C-001", 1000, "recurrente", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("bajo"));

        BigDecimal saldoDespues = saldoDe("C-001");
        assertThat(saldoDespues).isEqualByComparingTo(saldoAntes.subtract(new BigDecimal("1000.00")));
    }

    @Test
    void operacionConSaldoInsuficienteSeRechaza() throws Exception {
        BigDecimal saldo = saldoDe("C-001");
        double montoExcesivo = saldo.doubleValue() + 1000;

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(sesion("ADMIN", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("C-001", montoExcesivo, "recurrente", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.containsString("Saldo insuficiente")));
    }

    @Test
    void operacionBloqueadaNoDescuentaSaldo() throws Exception {
        BigDecimal saldoAntes = saldoDe("C-002");

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(sesion("ADMIN", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cliente": "C-002",
                                  "monto": 20000,
                                  "categoria": "cripto",
                                  "hora": 2,
                                  "pais": "alto_riesgo",
                                  "dispositivo": "nuevo",
                                  "operacionesUltimaHora": 6,
                                  "beneficiario": "nuevo",
                                  "ip": "nueva",
                                  "beneficiarioNombre": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("critico"));

        assertThat(saldoDe("C-002")).isEqualByComparingTo(saldoAntes);
    }

    private BigDecimal saldoDe(String codigoCliente) {
        Cuenta cuenta = cuentaRepository
                .findFirstByCliente_IdClienteOrderByIdCuentaAsc(
                        clienteRepository.findByCodigoCliente(codigoCliente).orElseThrow().getIdCliente())
                .orElseThrow();
        return cuenta.getSaldoDisponible();
    }
}

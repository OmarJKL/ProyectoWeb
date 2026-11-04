package com.lambdashield.fraude.presentation.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El dashboard expone cifras reales de PostgreSQL: saldo de cuentas activas,
 * ingresos/egresos del periodo, serie diaria y últimos movimientos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardApiMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpSession analista() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", "prueba-analista");
        session.setAttribute("nombreUsuario", "Usuario ANALISTA");
        session.setAttribute("rol", "ANALISTA");
        return session;
    }

    @Test
    void resumenTieneCifrasDelPeriodo() throws Exception {
        mockMvc.perform(get("/api/v1/metric/summary?days=365").session(analista()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").isNumber())
                .andExpect(jsonPath("$.ingresos").isNumber())
                .andExpect(jsonPath("$.egresos").isNumber())
                .andExpect(jsonPath("$.variacionSaldo").isNumber())
                .andExpect(jsonPath("$.variacionIngresos").isNumber())
                .andExpect(jsonPath("$.variacionEgresos").isNumber());
    }

    @Test
    void serieDiariaTieneUnPuntoPorDiaDelRango() throws Exception {
        mockMvc.perform(get("/api/v1/metric/movement?days=7").session(analista()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].fecha").isString())
                .andExpect(jsonPath("$[0].ingreso").isNumber())
                .andExpect(jsonPath("$[0].egreso").isNumber());
    }

    @Test
    void movimientosRecientesLimitanLaCantidad() throws Exception {
        mockMvc.perform(get("/api/v1/metric/recent?days=365").session(analista()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(8)));
    }
}

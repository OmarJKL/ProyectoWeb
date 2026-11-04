package com.lambdashield.fraude.presentation.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El endpoint de salud es público para balanceadores y monitoreo, pero no debe
 * exponer detalles de la base de datos ni del entorno.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SaludMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saludResponderSinSesionYSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void otrosEndpointsDeActuatorNoEstanExpuestos() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
    }
}

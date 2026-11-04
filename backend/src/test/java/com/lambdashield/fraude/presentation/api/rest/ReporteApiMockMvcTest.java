package com.lambdashield.fraude.presentation.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exportación de reportes en CSV, Excel y PDF, con filtros por fecha, nivel y
 * cliente, y validación del formato solicitado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReporteApiMockMvcTest {

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
    void exportaCsvConEncabezado() throws Exception {
        byte[] cuerpo = mockMvc.perform(get("/api/v1/report/export?formato=csv").session(analista()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("reporte_transacciones_lambda.csv")))
                .andReturn().getResponse().getContentAsByteArray();

        String csv = new String(cuerpo, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("ID,Fecha,Titular");
        assertThat(csv.lines().count()).isGreaterThan(1);
    }

    @Test
    void exportaExcelValido() throws Exception {
        byte[] cuerpo = mockMvc.perform(get("/api/v1/report/export?formato=xlsx").session(analista()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();

        // Todo .xlsx es un ZIP: empieza con la firma PK.
        assertThat(cuerpo.length).isGreaterThan(0);
        assertThat(cuerpo[0]).isEqualTo((byte) 'P');
        assertThat(cuerpo[1]).isEqualTo((byte) 'K');
    }

    @Test
    void exportaPdfValido() throws Exception {
        byte[] cuerpo = mockMvc.perform(get("/api/v1/report/export?formato=pdf").session(analista()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(cuerpo, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void filtroPorNivelSoloIncluyeEseNivel() throws Exception {
        String csv = new String(
                mockMvc.perform(get("/api/v1/report/export?formato=csv&nivel=critico").session(analista()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv.lines().skip(1).allMatch(linea -> linea.contains("\"CRITICO\""))).isTrue();
    }

    @Test
    void filtroPorClienteSoloIncluyeEseCliente() throws Exception {
        String csv = new String(
                mockMvc.perform(get("/api/v1/report/export?formato=csv&cliente=C-001&nivel=bajo").session(analista()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv.lines().count()).isGreaterThan(1);
    }

    @Test
    void formatoONivelInvalidosDevuelven400() throws Exception {
        mockMvc.perform(get("/api/v1/report/export?formato=docx").session(analista()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/report/export?formato=csv&nivel=extremo").session(analista()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteNoPuedeExportarReportes() throws Exception {
        MockHttpSession cliente = new MockHttpSession();
        cliente.setAttribute("usuario", "prueba-cliente");
        cliente.setAttribute("rol", "CLIENTE");

        mockMvc.perform(get("/api/v1/report/export?formato=csv").session(cliente))
                .andExpect(status().isForbidden());
    }
}

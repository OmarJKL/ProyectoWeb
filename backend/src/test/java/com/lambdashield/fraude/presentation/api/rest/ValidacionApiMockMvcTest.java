package com.lambdashield.fraude.presentation.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación de entrada de los endpoints REST: cada regla de formato, rango y
 * tipo se prueba con una sola violación para que el primer error sea determinista.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidacionApiMockMvcTest {

    private static final String MONTO_DECIMALES = "El monto admite hasta 16 enteros y 2 decimales";
    private static final String CATEGORIA = "La categoría solo admite letras y guion bajo (máximo 40 caracteres)";
    private static final String CLIENTE = "El código de cliente admite hasta 20 letras, números o guiones";
    private static final String OPERACIONES = "Las operaciones de la última hora deben estar entre 0 y 1000";
    private static final String RANGO_DIAS = "El rango debe estar entre 1 y 365 días";
    private static final String ID_POSITIVO = "El identificador debe ser positivo";
    private static final String CUERPO = "El cuerpo de la petición es obligatorio y debe ser JSON válido";

    private static final String JSON_VALIDO = """
            {
              "cliente": "C-001",
              "monto": 150.0,
              "categoria": "servicios",
              "hora": 12,
              "pais": "local",
              "dispositivo": "reconocido",
              "operacionesUltimaHora": 1,
              "beneficiario": "recurrente",
              "ip": "conocida"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpSession admin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", "prueba-admin");
        session.setAttribute("nombreUsuario", "Usuario ADMIN");
        session.setAttribute("rol", "ADMIN");
        return session;
    }

    @Test
    void requestValidoSigueAceptandose() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void montoConMasDeDosDecimalesSeRechaza() throws Exception {
        assertCampoInvalido(JSON_VALIDO.replace("150.0", "10.555"), MONTO_DECIMALES);
    }

    @Test
    void montoDemasiadoGrandeSeRechaza() throws Exception {
        assertCampoInvalido(JSON_VALIDO.replace("150.0", "1e17"), MONTO_DECIMALES);
    }

    @Test
    void categoriaConCaracteresPeligrososSeRechaza() throws Exception {
        assertCampoInvalido(JSON_VALIDO.replace("\"servicios\"", "\"retail; DROP TABLE\""), CATEGORIA);
    }

    @Test
    void categoriaDemasiadoLargaSeRechaza() throws Exception {
        String larga = "a".repeat(41);
        assertCampoInvalido(JSON_VALIDO.replace("\"servicios\"", "\"" + larga + "\""), CATEGORIA);
    }

    @Test
    void clienteConFormatoInvalidoSeRechaza() throws Exception {
        assertCampoInvalido(JSON_VALIDO.replace("\"C-001\"", "\"C 001!\""), CLIENTE);
    }

    @Test
    void operacionesUltimaHoraFueraDeRangoSeRechaza() throws Exception {
        assertCampoInvalido(JSON_VALIDO.replace("\"operacionesUltimaHora\": 1", "\"operacionesUltimaHora\": 1001"),
                OPERACIONES);
    }

    @Test
    void jsonMalformadoDevuelve400NoRompeElServidor() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(CUERPO));
    }

    @Test
    void cuerpoVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(CUERPO));
    }

    @Test
    void contentTypeNoSoportadoDevuelve415() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(JSON_VALIDO))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void idNoNumericoEnPatchDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/evaluation/{id}", "abc").session(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("El parámetro 'id' tiene un valor inválido"));
    }

    @Test
    void idNoPositivoEnPatchDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/evaluation/{id}", "0").session(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(ID_POSITIVO));
    }

    @Test
    void diasCeroOExcesivosDevuelven400() throws Exception {
        for (String uri : new String[]{"/api/v1/metric/summary?days=0",
                "/api/v1/metric/movement?days=366",
                "/api/v1/metric/recent?days=-5"}) {
            mockMvc.perform(get(uri).session(admin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0]").value(RANGO_DIAS));
        }
    }

    @Test
    void diasNoNumericosDevuelven400() throws Exception {
        mockMvc.perform(get("/api/v1/metric/movement?days=mes").session(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("El parámetro 'days' tiene un valor inválido"));
    }

    @Test
    void diasEnRangoSigueFuncionando() throws Exception {
        mockMvc.perform(get("/api/v1/metric/summary?days=30").session(admin()))
                .andExpect(status().isOk());
    }

    private void assertCampoInvalido(String json, String mensaje) throws Exception {
        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errores[0]").value(mensaje));
    }
}

package com.lambdashield.fraude.controller;

import com.lambdashield.fraude.model.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.model.response.EvaluacionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas MockMvc de autenticación, autorización por rol, validación,
 * soft delete con PATCH y aislamiento de datos del portal del cliente.
 *
 * El contexto completo usa H2 en modo PostgreSQL y el DataInitializer real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadApiMockMvcTest {

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

    @Autowired
    private EvaluacionFraudeRepository evaluacionFraudeRepository;

    private MockHttpSession cliente() {
        return sesion("CLIENTE", "C-001");
    }

    private MockHttpSession analista() {
        return sesion("ANALISTA", null);
    }

    private MockHttpSession admin() {
        return sesion("ADMIN", null);
    }

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

    @Test
    void apiSinSesionDevuelve401Json() throws Exception {
        mockMvc.perform(get("/api/v1/rule"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("No autenticado"));

        mockMvc.perform(get("/api/v1/evaluation"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void clienteNoPuedeAccederAEndpointsDeAdministracion() throws Exception {
        mockMvc.perform(get("/api/v1/metric").session(cliente()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));

        mockMvc.perform(get("/api/v1/rule").session(cliente()))
                .andExpect(status().isForbidden());
    }

    @Test
    void analistaPuedeLeerPeroNoPuedeDesactivar() throws Exception {
        mockMvc.perform(get("/api/v1/rule").session(analista()))
                .andExpect(status().isOk());

        Long id = evaluacionFraudeRepository.findByEsActivaTrueOrderByFechaEvaluacionDesc()
                .getFirst().getIdEvaluacion();

        mockMvc.perform(patch("/api/v1/evaluation/{id}", id).session(analista()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    @Test
    void adminDesactivaEvaluacionConPatchSinBorrarla() throws Exception {
        Long id = evaluacionFraudeRepository.findByEsActivaTrueOrderByFechaEvaluacionDesc()
                .getFirst().getIdEvaluacion();

        mockMvc.perform(patch("/api/v1/evaluation/{id}", id).session(admin()))
                .andExpect(status().isNoContent());

        assertThat(evaluacionFraudeRepository.findById(id))
                .isPresent()
                .get()
                .extracting(e -> e.isEsActiva())
                .isEqualTo(false);
    }

    @Test
    void deleteNoEstaPermitidoNiParaAdmin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/evaluation").session(admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errores[0]").value("Método no permitido; use PATCH para la baja lógica"));
    }

    @Test
    void requestInvalidoDevuelve400ConListaDeErrores() throws Exception {
        String invalido = """
                {
                  "cliente": "",
                  "monto": -5,
                  "categoria": "retail",
                  "hora": 24,
                  "pais": "marte",
                  "dispositivo": "otro",
                  "operacionesUltimaHora": -1,
                  "beneficiario": "otro",
                  "ip": "otra"
                }
                """;

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.errores[0]").isString());
    }

    @Test
    void clienteInexistenteDevuelve404() throws Exception {
        String inexistente = JSON_VALIDO.replace("\"C-001\"", "\"C-999\"");

        mockMvc.perform(post("/api/v1/evaluation")
                        .session(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errores[0]").value("Cliente no encontrado: C-999"));
    }

    @Test
    void operacionSoloMuestraTransaccionesDelClienteAutenticado() throws Exception {
        MvcResult result = mockMvc.perform(get("/operacion").session(cliente()))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/operacion"))
                .andReturn();

        List<EvaluacionResponse> transacciones = transaccionesDelModelo(result);

        assertThat(transacciones).isNotEmpty();
        assertThat(transacciones).allMatch(t -> "C-001".equals(t.clienteCodigo()));
        assertThat(transacciones).noneMatch(t -> "C-002".equals(t.clienteCodigo()));
    }

    @Test
    void operacionSinCodigoClienteNoHaceFallbackATodasLasTransacciones() throws Exception {
        MockHttpSession clienteSinCodigo = sesion("CLIENTE", null);

        MvcResult result = mockMvc.perform(get("/operacion").session(clienteSinCodigo))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(transaccionesDelModelo(result)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static List<EvaluacionResponse> transaccionesDelModelo(MvcResult result) {
        return (List<EvaluacionResponse>) result.getModelAndView()
                .getModel()
                .get("transacciones");
    }
}

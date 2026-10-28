package com.lambdashield.fraude.controller;

import com.lambdashield.fraude.controller.api.EvaluationApiController;
import com.lambdashield.fraude.controller.api.MetricApiController;
import com.lambdashield.fraude.controller.api.ReportApiController;
import com.lambdashield.fraude.controller.api.RuleApiController;
import com.lambdashield.fraude.controller.config.AuthInterceptor;
import com.lambdashield.fraude.controller.view.OperacionClienteController;
import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.rule.ReglaBeneficiario;
import com.lambdashield.fraude.model.rule.ReglaDispositivo;
import com.lambdashield.fraude.model.rule.ReglaHorario;
import com.lambdashield.fraude.model.rule.ReglaIp;
import com.lambdashield.fraude.model.rule.ReglaMontoInusual;
import com.lambdashield.fraude.model.rule.ReglaUbicacion;
import com.lambdashield.fraude.model.rule.ReglaVelocidad;
import com.lambdashield.fraude.model.rule.ReglaViajeImposible;
import com.lambdashield.fraude.model.service.DashboardService;
import com.lambdashield.fraude.model.service.DeteccionFraudeService;
import com.lambdashield.fraude.model.service.ReporteService;
import com.lambdashield.fraude.model.service.TransaccionService;
import com.lambdashield.fraude.model.store.ClienteStore;
import com.lambdashield.fraude.model.store.EvaluacionStore;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas MockMvc de autenticación, autorización por rol, validación y aislamiento
 * de datos del portal del cliente.
 *
 * Se usa un contexto standalone con los controllers, servicios y stores reales
 * para no depender de PostgreSQL (la aplicación usa stores en memoria).
 */
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

    private MockMvc mockMvc;
    private MockHttpSession cliente;
    private MockHttpSession analista;
    private MockHttpSession admin;

    @BeforeEach
    void setUp() {
        ClienteStore clienteStore = new ClienteStore();
        EvaluacionStore evaluacionStore = new EvaluacionStore();
        evaluacionStore.inicializarDatosPrueba();

        DeteccionFraudeService deteccion = new DeteccionFraudeService(List.of(
                new ReglaMontoInusual(),
                new ReglaDispositivo(),
                new ReglaIp(),
                new ReglaUbicacion(),
                new ReglaHorario(),
                new ReglaVelocidad(),
                new ReglaViajeImposible(),
                new ReglaBeneficiario()
        ));
        TransaccionService transaccionService =
                new TransaccionService(deteccion, evaluacionStore, clienteStore);
        DashboardService dashboardService = new DashboardService(evaluacionStore);
        ReporteService reporteService = new ReporteService(evaluacionStore);

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new EvaluationApiController(transaccionService),
                        new MetricApiController(transaccionService, dashboardService),
                        new RuleApiController(),
                        new ReportApiController(reporteService),
                        new OperacionClienteController(clienteStore, transaccionService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(validator))
                .addInterceptors(new AuthInterceptor())
                .build();

        cliente = sesion("CLIENTE", "C-001");
        analista = sesion("ANALISTA", null);
        admin = sesion("ADMIN", null);
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
        mockMvc.perform(get("/api/v1/metric").session(cliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));

        mockMvc.perform(get("/api/v1/rule").session(cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void analistaPuedeLeerPeroNoEliminar() throws Exception {
        mockMvc.perform(get("/api/v1/rule").session(analista))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/evaluation").session(analista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    @Test
    void adminPuedeEliminarHistorial() throws Exception {
        mockMvc.perform(delete("/api/v1/evaluation").session(admin))
                .andExpect(status().isNoContent());
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
                        .session(admin)
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
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errores[0]").value("Cliente no encontrado: C-999"));
    }

    @Test
    void operacionSoloMuestraTransaccionesDelClienteAutenticado() throws Exception {
        MvcResult result = mockMvc.perform(get("/operacion").session(cliente))
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

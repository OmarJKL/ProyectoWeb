package com.lambdashield.fraude.presentation.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de usuarios del sistema: altas, cambios, bajas físicas, validación de
 * entrada y reglas de negocio (unicidad, último administrador, propio usuario).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioApiMockMvcTest {

    private static final String JSON_NUEVO = """
            {
              "usuario": "nuevo.usuario",
              "nombres": "Usuario Nuevo",
              "correo": "nuevo.usuario@lambda.local",
              "clave": "claveSegura123",
              "rol": "ANALISTA",
              "codigoCliente": null
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpSession sesion(String rol) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", "prueba-" + rol.toLowerCase());
        session.setAttribute("nombreUsuario", "Usuario " + rol);
        session.setAttribute("rol", rol);
        return session;
    }

    @Test
    void adminListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/user").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuario").isString())
                .andExpect(jsonPath("$[?(@.usuario == 'admin')]").isNotEmpty());
    }

    @Test
    void adminCreaUsuarioYapareceEnElListado() throws Exception {
        mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("nuevo.usuario"))
                .andExpect(jsonPath("$.rol").value("ANALISTA"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(get("/api/v1/user").session(sesion("ADMIN")))
                .andExpect(jsonPath("$[?(@.usuario == 'nuevo.usuario')]").isNotEmpty());
    }

    @Test
    void usuarioDuplicadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO.replace("nuevo.usuario", "admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("El nombre de usuario ya existe: admin"));
    }

    @Test
    void actualizaUsuarioYCambiaSuClave() throws Exception {
        String creado = mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = extraerId(creado);

        mockMvc.perform(patch("/api/v1/user/{id}", id)
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Usuario Editado",
                                  "correo": "nuevo.usuario@lambda.local",
                                  "estado": "BLOQUEADO",
                                  "rol": "ANALISTA",
                                  "codigoCliente": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Usuario Editado"))
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));

        mockMvc.perform(patch("/api/v1/user/{id}/clave", id)
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clave\": \"otraClave123\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminEliminaUsuarioCreado() throws Exception {
        String creado = mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = extraerId(creado);

        mockMvc.perform(delete("/api/v1/user/{id}", id).session(sesion("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/user/{id}", id).session(sesion("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void noPuedeEliminarAlUltimoAdministrador() throws Exception {
        mockMvc.perform(delete("/api/v1/user/1").session(sesion("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("No se puede eliminar al último administrador activo"));
    }

    @Test
    void analistaYClienteNoGestionanUsuarios() throws Exception {
        for (MockHttpSession sesion : new MockHttpSession[]{sesion("ANALISTA"), sesion("CLIENTE")}) {
            mockMvc.perform(get("/api/v1/user").session(sesion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/user")
                            .session(sesion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSON_NUEVO))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/user/1").session(sesion))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void validacionDeEntradaRechazaClaveCortaYRolInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO.replace("claveSegura123", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());

        mockMvc.perform(post("/api/v1/user")
                        .session(sesion("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NUEVO.replace("ANALISTA", "SUPERVISOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/user/999999").session(sesion("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private static long extraerId(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}

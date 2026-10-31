package com.lambdashield.fraude.presentation.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifica contra el contexto MVC real (interceptor + controller + Thymeleaf)
 * que el portal del cliente solo renderiza sus propias operaciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OperacionClienteViewMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void operacionNoMuestraTransaccionesDeOtrosClientes() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", "usuario");
        session.setAttribute("nombreUsuario", "María Fernández (Cliente)");
        session.setAttribute("rol", "CLIENTE");
        session.setAttribute("codigoCliente", "C-001");

        MvcResult result = mockMvc.perform(get("/operacion").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/operacion"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // Operaciones semilla de C-001.
        assertThat(html).contains("TR-00001", "TR-00002", "TR-00005");
        // Operaciones semilla de C-002 y C-003: no deben aparecer.
        assertThat(html).doesNotContain(
                "TR-00003", "TR-00004", "TR-00006", "TR-00007",
                "TR-00008", "TR-00009", "TR-00010"
        );
    }
}

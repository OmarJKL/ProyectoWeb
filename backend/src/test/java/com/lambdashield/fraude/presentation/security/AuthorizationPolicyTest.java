package com.lambdashield.fraude.presentation.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationPolicyTest {

    private final AuthorizationPolicy policy = new AuthorizationPolicy();

    @Test
    void clienteSoloPuedeCrearEvaluacionesPropias() {
        assertThat(policy.rechazoApi("CLIENTE", "POST", "/api/v1/evaluation")).isEmpty();
        assertThat(policy.rechazoApi("CLIENTE", "GET", "/api/v1/evaluation"))
                .contains("No autorizado");
    }

    @Test
    void analistaSoloPuedeLeerYAdminNoPuedeBorrarFisicamente() {
        assertThat(policy.rechazoApi("ANALISTA", "GET", "/api/v1/metric")).isEmpty();
        assertThat(policy.rechazoApi("ANALISTA", "PATCH", "/api/v1/evaluation/1"))
                .contains("No autorizado");
        assertThat(policy.rechazoApi("ADMIN", "DELETE", "/api/v1/evaluation/1"))
                .contains("DELETE no permitido; use PATCH para desactivar");
    }

    @Test
    void clientesYAnalistasSoloAccedenASusPaginas() {
        assertThat(policy.redireccionPagina("CLIENTE", "/operacion")).isEmpty();
        assertThat(policy.redireccionPagina("CLIENTE", "/reportes")).contains("/operacion");
        assertThat(policy.redireccionPagina("ANALISTA", "/simulador")).contains("/");
        assertThat(policy.redireccionPagina("ANALISTA", "/reportes")).isEmpty();
        assertThat(policy.redireccionPagina(null, "/reportes")).contains("/login");
    }
}

package com.lambdashield.fraude.presentation.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Política de acceso por rol, desacoplada de Servlet y de la escritura de respuestas HTTP. */
@Component
public class AuthorizationPolicy {

    private static final String CLIENTE = "CLIENTE";
    private static final String ANALISTA = "ANALISTA";
    private static final String ADMIN = "ADMIN";

    /** Devuelve el mensaje de rechazo para una API, o vacío cuando el rol puede continuar. */
    public Optional<String> rechazoApi(String rol, String metodo, String ruta) {
        if (HttpMethod.DELETE.matches(metodo)) {
            return Optional.of("DELETE no permitido; use PATCH para desactivar");
        }
        if (ADMIN.equals(rol)) return Optional.empty();
        if (ANALISTA.equals(rol) && HttpMethod.GET.matches(metodo)) return Optional.empty();
        if (CLIENTE.equals(rol)
                && HttpMethod.POST.matches(metodo)
                && "/api/v1/evaluation".equals(ruta)) {
            return Optional.empty();
        }
        return Optional.of("No autorizado");
    }

    /** Devuelve la ruta de redirección para una página, o vacío cuando el rol puede continuar. */
    public Optional<String> redireccionPagina(String rol, String ruta) {
        if (ADMIN.equals(rol)) return Optional.empty();

        if (ANALISTA.equals(rol)) {
            return esPaginaCliente(ruta) || esPaginaSimulador(ruta)
                    ? Optional.of("/")
                    : Optional.empty();
        }

        if (CLIENTE.equals(rol)) {
            return esPaginaCliente(ruta) ? Optional.empty() : Optional.of("/operacion");
        }
        return Optional.of("/login");
    }

    private static boolean esPaginaCliente(String ruta) {
        return ruta.equals("/operacion") || ruta.startsWith("/operacion/");
    }

    private static boolean esPaginaSimulador(String ruta) {
        return ruta.equals("/simulador") || ruta.startsWith("/simulador/");
    }
}

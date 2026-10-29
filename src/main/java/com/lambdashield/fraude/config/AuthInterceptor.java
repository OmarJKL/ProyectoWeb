package com.lambdashield.fraude.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Interceptor de sesión y autorización.
 *
 * Reglas:
 *  - Sin sesión: las rutas /api/** responden 401 JSON y las páginas redirigen a /login.
 *  - CLIENTE: solo POST /api/v1/evaluation y la página /operacion. El resto de /api/** → 403 JSON.
 *  - ANALISTA: solo lectura (GET) de /api/** y páginas de administración. Nada de POST/PATCH.
 *  - ADMIN: acceso total, incluido PATCH /api/v1/evaluation/{id}.
 *  - DELETE no se permite en ninguna ruta: la baja lógica usa PATCH.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String ROL_ANALISTA = "ANALISTA";
    private static final String ROL_ADMIN = "ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = obtenerRuta(request);

        // Recursos estáticos y rutas públicas
        if (esRecursoPublico(uri)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            if (esRutaApi(uri)) {
                return rechazar(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado");
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String rol = (String) session.getAttribute("rol");

        if (esRutaApi(uri)) {
            return autorizarApi(request, response, rol, uri);
        }
        return autorizarPagina(request, response, rol, uri);
    }

    private String obtenerRuta(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        int puntoYComa = uri.indexOf(';');
        return puntoYComa >= 0 ? uri.substring(0, puntoYComa) : uri;
    }

    private boolean esRecursoPublico(String uri) {
        return uri.startsWith("/login") || uri.startsWith("/logout")
                || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/error");
    }

    private boolean esRutaApi(String uri) {
        return uri.equals("/api") || uri.startsWith("/api/");
    }

    private boolean autorizarApi(HttpServletRequest request, HttpServletResponse response,
                                 String rol, String uri) throws IOException {
        // Política global: no se aceptan borrados físicos. La baja lógica usa PATCH.
        if (HttpMethod.DELETE.matches(request.getMethod())) {
            return rechazar(response, HttpServletResponse.SC_FORBIDDEN,
                    "DELETE no permitido; use PATCH para desactivar");
        }

        if (ROL_ADMIN.equals(rol)) {
            return true;
        }

        if (ROL_ANALISTA.equals(rol)) {
            // Lectura de todo el catálogo de APIs, sin modificaciones.
            if (HttpMethod.GET.matches(request.getMethod())) {
                return true;
            }
            return rechazar(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
        }

        if (ROL_CLIENTE.equals(rol)) {
            // El cliente solo puede evaluar sus propias operaciones.
            if (HttpMethod.POST.matches(request.getMethod()) && uri.equals("/api/v1/evaluation")) {
                return true;
            }
            return rechazar(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
        }

        return rechazar(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
    }

    private boolean autorizarPagina(HttpServletRequest request, HttpServletResponse response,
                                    String rol, String uri) throws IOException {
        if (ROL_ADMIN.equals(rol)) {
            return true;
        }

        if (ROL_ANALISTA.equals(rol)) {
            // El analista ve las páginas de administración, pero no el portal del cliente
            // ni el simulador (su única acción es POST, que le está prohibida).
            if (esPaginaCliente(uri) || esPaginaSimulador(uri)) {
                response.sendRedirect(request.getContextPath() + "/");
                return false;
            }
            return true;
        }

        if (ROL_CLIENTE.equals(rol)) {
            if (esPaginaCliente(uri)) {
                return true;
            }
            response.sendRedirect(request.getContextPath() + "/operacion");
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private boolean esPaginaCliente(String uri) {
        return uri.equals("/operacion") || uri.startsWith("/operacion/");
    }

    private boolean esPaginaSimulador(String uri) {
        return uri.equals("/simulador") || uri.startsWith("/simulador/");
    }

    private boolean rechazar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        return false;
    }
}

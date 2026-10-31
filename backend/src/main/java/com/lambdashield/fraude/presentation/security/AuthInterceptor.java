package com.lambdashield.fraude.presentation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

    private final AuthorizationPolicy authorizationPolicy;

    public AuthInterceptor(AuthorizationPolicy authorizationPolicy) {
        this.authorizationPolicy = authorizationPolicy;
    }

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
                                 String rol, String uri) throws java.io.IOException {
        var rechazo = authorizationPolicy.rechazoApi(rol, request.getMethod(), uri);
        if (rechazo.isPresent()) {
            return rechazar(response, HttpServletResponse.SC_FORBIDDEN, rechazo.get());
        }
        return true;
    }

    private boolean autorizarPagina(HttpServletRequest request, HttpServletResponse response,
                                    String rol, String uri) throws java.io.IOException {
        var redireccion = authorizationPolicy.redireccionPagina(rol, uri);
        if (redireccion.isPresent()) {
            response.sendRedirect(request.getContextPath() + redireccion.get());
            return false;
        }
        return true;
    }

    private boolean rechazar(HttpServletResponse response, int estado, String mensaje)
            throws java.io.IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        return false;
    }
}

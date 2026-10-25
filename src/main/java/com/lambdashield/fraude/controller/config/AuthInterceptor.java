package com.lambdashield.fraude.controller.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_ANALISTA = "ANALISTA";
    private static final String ROL_CLIENTE = "CLIENTE";

    private static final String RUTA_OPERACION = "/operacion";
    private static final String RUTA_EVALUACION = "/api/v1/evaluation";

    /** Rutas HTML a las que puede acceder un cliente. */
    private static final Set<String> PAGINAS_CLIENTE = Set.of(RUTA_OPERACION);

    /** Rutas HTML de administración (analistas y administradores). */
    private static final Set<String> PAGINAS_ADMIN = Set.of(
            "/", "/simulador", "/transacciones", "/reglas", "/reportes");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Recursos estáticos y rutas públicas
        if (uri.startsWith("/login") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/error") || uri.startsWith("/logout")) {
            return true;
        }

        boolean api = esApi(uri);
        HttpSession session = request.getSession(false);
        boolean autenticado = session != null && session.getAttribute("usuario") != null;

        if (!autenticado) {
            if (api) {
                return responderJson(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado");
            }
            return redirigir(response, "/login");
        }

        String rol = (String) session.getAttribute("rol");

        if (ROL_ADMIN.equals(rol)) {
            return true;
        }

        if (ROL_CLIENTE.equals(rol)) {
            if (api) {
                if (puedeClienteApi(uri, request.getMethod())) {
                    return true;
                }
                return responderJson(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
            }
            if (coincideRuta(uri, PAGINAS_CLIENTE)) {
                return true;
            }
            return redirigir(response, RUTA_OPERACION);
        }

        if (ROL_ANALISTA.equals(rol)) {
            if (api) {
                if (puedeAnalistaApi(uri, request.getMethod())) {
                    return true;
                }
                return responderJson(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
            }
            if (coincideRuta(uri, PAGINAS_ADMIN)) {
                return true;
            }
            return redirigir(response, "/");
        }

        // Rol desconocido: se deniega el acceso.
        if (api) {
            return responderJson(response, HttpServletResponse.SC_FORBIDDEN, "No autorizado");
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }

    /** El cliente solo puede evaluar transacciones desde su propio portal. */
    private boolean puedeClienteApi(String uri, String metodo) {
        return "POST".equalsIgnoreCase(metodo) && RUTA_EVALUACION.equals(normalizar(uri));
    }

    /** El analista solo puede consultar (GET/HEAD) los recursos de monitoreo. */
    private boolean puedeAnalistaApi(String uri, String metodo) {
        if (!"GET".equalsIgnoreCase(metodo) && !"HEAD".equalsIgnoreCase(metodo)) {
            return false;
        }
        String ruta = normalizar(uri);
        return (ruta.equals(RUTA_EVALUACION) || ruta.startsWith(RUTA_EVALUACION + "/"))
                || ruta.equals("/api/v1/metric") || ruta.startsWith("/api/v1/metric/")
                || ruta.equals("/api/v1/rule") || ruta.startsWith("/api/v1/rule/")
                || ruta.equals("/api/v1/report") || ruta.startsWith("/api/v1/report/");
    }

    private boolean esApi(String uri) {
        return uri.equals("/api") || uri.startsWith("/api/");
    }

    private boolean coincideRuta(String uri, Set<String> rutasPermitidas) {
        String ruta = normalizar(uri);
        return rutasPermitidas.contains(ruta)
                || rutasPermitidas.stream().anyMatch(p -> !p.equals("/") && ruta.startsWith(p + "/"));
    }

    private String normalizar(String uri) {
        if (uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private boolean redirigir(HttpServletResponse response, String destino) throws IOException {
        response.sendRedirect(destino);
        return false;
    }

    private boolean responderJson(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        response.getWriter().flush();
        return false;
    }
}

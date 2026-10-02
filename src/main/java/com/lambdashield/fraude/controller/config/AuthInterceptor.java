package com.lambdashield.fraude.controller.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Recursos estáticos y rutas públicas
        if (uri.startsWith("/login") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/error") || uri.startsWith("/logout")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect("/login");
            return false;
        }

        String rol = (String) session.getAttribute("rol");

        // Si es usuario estándar (CLIENTE) y trata de entrar a rutas de administración
        if ("CLIENTE".equals(rol)) {
            if (uri.equals("/") || uri.startsWith("/transacciones") || uri.startsWith("/reglas")
                    || uri.startsWith("/reportes") || uri.startsWith("/simulador")
                    || uri.startsWith("/api/v1/dashboard") || uri.startsWith("/api/v1/reglas")
                    || uri.startsWith("/api/v1/reportes")) {
                response.sendRedirect("/operacion");
                return false;
            }
        }

        return true;
    }
}

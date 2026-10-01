package com.lambdashield.fraude.view;

/**
 * ============================================================
 *  CAPA VISTA — Lambda Shield · Fraud Detection System
 * ============================================================
 *
 * En la arquitectura MVC de este proyecto, la VISTA está
 * implementada mediante plantillas Thymeleaf ubicadas en:
 *
 *   src/main/resources/templates/
 *   ├── login.html              → Pantalla de acceso al sistema
 *   ├── dashboard.html          → Panel principal del administrador
 *   ├── transacciones.html      → Historial y KPIs de transacciones
 *   ├── simulador.html          → Simulador de evaluación antifraude
 *   ├── reglas.html             → Reglas del motor (RF001–RF008)
 *   ├── reportes.html           → Reportes ejecutivos y exportación CSV
 *   └── cliente/
 *       └── operacion.html      → Panel del usuario estándar (cliente)
 *
 * Spring Boot + Thymeleaf requiere que las vistas HTML estén
 * en resources/templates/. Este paquete existe para hacer
 * visible la capa Vista en la estructura de paquetes Java.
 *
 * @see <a href="../../../../../../../resources/templates/">templates/</a>
 */
public final class ViewLayer {
    private ViewLayer() {
        // Clase de documentación,
    }
}
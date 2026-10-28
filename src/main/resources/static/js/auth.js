(() => {
    'use strict';

    function mostrarAviso(mensaje) {
        let contenedor = document.getElementById('contenedorAlertas');
        if (!contenedor) {
            contenedor = document.createElement('div');
            contenedor.id = 'contenedorAlertas';
            contenedor.className = 'contenedor-alertas position-fixed';
            document.body.appendChild(contenedor);
        }

        const aviso = document.createElement('div');
        aviso.className = 'alerta-ls';
        aviso.setAttribute('role', 'alert');
        aviso.textContent = mensaje;
        contenedor.appendChild(aviso);

        setTimeout(() => {
            aviso.style.opacity = '0';
            aviso.style.transition = 'opacity 0.4s ease';
            setTimeout(() => aviso.remove(), 400);
        }, 5000);
    }

    /**
     * Envoltorio de fetch que centraliza el manejo de sesión:
     *  - 401 → redirige a /login
     *  - 403 → muestra un aviso al usuario y lanza el error
     */
    async function fetchApi(url, opciones = {}) {
        const res = await fetch(url, opciones);

        if (res.status === 401) {
            window.location.replace('/login');
            throw new Error('Sesión expirada');
        }

        if (res.status === 403) {
            let mensaje = 'No tienes permisos para realizar esta acción.';
            try {
                const data = await res.clone().json();
                if (data && data.error) {
                    mensaje = data.error;
                }
            } catch (e) {
                // Respuesta sin cuerpo JSON: se conserva el mensaje genérico.
            }
            mostrarAviso(mensaje);
            throw new Error(mensaje);
        }

        return res;
    }

    window.LSApi = { fetch: fetchApi, mostrarAviso };

    document.addEventListener('DOMContentLoaded', () => {
        const btnLogout = document.getElementById('btnCerrarSesion');
        if (btnLogout) {
            btnLogout.addEventListener('click', () => {
                window.location.href = '/logout';
            });
        }
    });
})();

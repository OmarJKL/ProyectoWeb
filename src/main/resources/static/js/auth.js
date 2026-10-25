(function () {
    'use strict';

    const MENSAJE_SIN_PERMISO = 'No tienes permisos para realizar esta acción.';

    /**
     * Realiza una petición conservando la cookie de sesión y centraliza
     * el tratamiento de respuestas 401 (sesión no válida) y 403 (sin permisos).
     */
    window.fetchConSesion = async function (url, opciones) {
        const configuracion = Object.assign({ credentials: 'same-origin' }, opciones || {});
        const respuesta = await fetch(url, configuracion);

        if (respuesta.status === 401) {
            window.location.replace('/login');
            throw new Error('Sesión no válida');
        }

        if (respuesta.status === 403) {
            window.alert(MENSAJE_SIN_PERMISO);
            throw new Error('Acceso denegado');
        }

        return respuesta;
    };

    document.addEventListener('DOMContentLoaded', () => {
        const btnLogout = document.getElementById('btnCerrarSesion');
        if (btnLogout) {
            btnLogout.addEventListener('click', () => {
                window.location.href = '/logout';
            });
        }
    });
})();

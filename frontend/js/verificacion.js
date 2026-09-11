const CLAVE_AUTENTICACION_BL = 'autenticacionLambdaShield';

// Usuarios de demostración. Cambia esto por tu propia validación real para el proyecto.
const USUARIOS_BL = [
    { usuario: 'admin',     clave: 'lambda2026' },
    { usuario: 'analista',  clave: 'fraude123'  }
];

/**
 * Verifica las credenciales contra la lista de usuarios de demo.
 * @param {string} usuario - El nombre de usuario ingresado
 * @param {string} clave - La contraseña ingresada
 * @returns {boolean} - Verdadero si las credenciales coinciden
 */
function blIniciarSesion(usuario, clave) {
    return USUARIOS_BL.some(u => u.usuario === usuario && u.clave === clave);
}


function blVerificarAutenticacion(rutaLogin = 'login.html') {
    if (sessionStorage.getItem(CLAVE_AUTENTICACION_BL) !== 'true') {
        window.location.replace(rutaLogin);
    }
}

/**
 * Cierra la sesión eliminando el registro del navegador y regresa al login.
 */
function blCerrarSesion() {
    sessionStorage.removeItem(CLAVE_AUTENTICACION_BL);
    const estaEnPaginaInterna = window.location.pathname.includes('/frontend/html/');
    window.location.replace(estaEnPaginaInterna ? 'login.html' : 'frontend/html/login.html');
}

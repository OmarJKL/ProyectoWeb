/* ==========================================================================
   LAMBDA SHIELD — Autenticación (demo, solo del lado del cliente)
   IMPORTANTE: esto es una verificación en el navegador con sessionStorage,
   pensada para una demo/proyecto. No reemplaza un login real con backend:
   cualquiera que sepa JavaScript podría saltarla editando la consola.
   Para producción, esto debe validarse contra un servidor (API + tokens).
   ========================================================================== */

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

/**
 * Si no hay una sesión activa, redirige de inmediato a login.html.
 * Debe llamarse lo antes posible en el <head> de cada página protegida (antes de
 * pintar el contenido) para evitar que se vea el panel sin autenticar.
 */
function blVerificarAutenticacion() {
    if (sessionStorage.getItem(CLAVE_AUTENTICACION_BL) !== 'true') {
        window.location.replace('login.html');
    }
}

/**
 * Cierra la sesión eliminando el registro del navegador y regresa al login.
 */
function blCerrarSesion() {
    sessionStorage.removeItem(CLAVE_AUTENTICACION_BL);
    window.location.replace('login.html');
}

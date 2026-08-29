/* ==========================================================================
   LAMBDA SHIELD — Autenticación (demo, solo del lado del cliente)
   IMPORTANTE: esto es una verificación en el navegador con sessionStorage,
   pensada para una demo/portafolio. No reemplaza un login real con backend:
   cualquiera que sepa JavaScript podría saltarla editando la consola.
   Para producción, esto debe validarse contra un servidor (API + tokens).
   ========================================================================== */

const LS_AUTH_KEY = 'lambdaShieldAuth';

// Usuarios de demostración. Cambia esto por tu propia validación real.
const LS_USERS = [
    { user: 'admin',     pass: 'lambda2026' },
    { user: 'analista',  pass: 'fraude123'  }
];

/**
 * Verifica las credenciales contra la lista de usuarios de demo.
 */
function lsLogin(user, pass) {
    return LS_USERS.some(u => u.user === user && u.pass === pass);
}

/**
 * Si no hay una sesión activa, redirige de inmediato a login.html.
 * Debe llamarse lo antes posible en cada página protegida (antes de
 * pintar el contenido) para evitar que se vea el panel sin autenticar.
 */
function lsCheckAuth() {
    if (sessionStorage.getItem(LS_AUTH_KEY) !== 'true') {
        window.location.replace('login.html');
    }
}

/**
 * Cierra la sesión y regresa al login.
 */
function lsLogout() {
    sessionStorage.removeItem(LS_AUTH_KEY);
    window.location.replace('login.html');
}

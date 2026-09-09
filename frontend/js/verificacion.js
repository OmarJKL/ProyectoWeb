/* ==========================================================================
   LAMBDA SHIELD — Autenticación y Control de Acceso por Roles (RBAC)
   --------------------------------------------------------------------------
   Manejo de sesión del lado del cliente mediante sessionStorage.
   Soporta roles:
     - 'ADMIN': Administrador con acceso al centro operativo y analítico
     - 'USER' : Usuario estándar restringido al flujo de transferencias
   ========================================================================== */

const CLAVE_AUTENTICACION_BL = 'autenticacionLambdaShield';
const CLAVE_USUARIO_ACTIVO_BL = 'usuarioActivoLambdaShield';

// Base de usuarios de demostración con roles y perfiles
const USUARIOS_BL = [
    {
        usuario: 'admin',
        clave: 'lambda2026',
        rol: 'ADMIN',
        nombre: 'Administrador del Sistema',
        email: 'admin@lambdashield.pe'
    },
    {
        usuario: 'analista',
        clave: 'fraude123',
        rol: 'ADMIN',
        nombre: 'Analista de Fraude',
        email: 'analista@lambdashield.pe'
    },
    {
        usuario: 'usuario',
        clave: 'user123',
        rol: 'USER',
        nombre: 'María Fernández',
        clienteId: 'C-001',
        tipoDocumento: 'DNI',
        numeroDocumento: '47891234',
        numeroCuenta: '191-4829103-0-45',
        tipoCuenta: 'Cuenta Sueldo Ahorros (Soles)',
        saldo: 24180.50,
        promedioCliente: 180,
        email: 'm.fernandez@bancolambda.pe'
    },
    {
        usuario: 'cliente2',
        clave: 'user123',
        rol: 'USER',
        nombre: 'Jorge Salas',
        clienteId: 'C-002',
        tipoDocumento: 'DNI',
        numeroDocumento: '41209845',
        numeroCuenta: '191-8930124-1-12',
        tipoCuenta: 'Cuenta Ahorros Premium (Soles)',
        saldo: 15420.00,
        promedioCliente: 950,
        email: 'j.salas@bancolambda.pe'
    }
];

/**
 * Autentica un usuario contra la lista de credenciales.
 * @param {string} usuario - Nombre de usuario
 * @param {string} clave - Contraseña ingresada
 * @returns {Object|null} - Objeto del usuario (sin contraseña) o null si es inválido
 */
function blAutenticar(usuario, clave) {
    const encontrado = USUARIOS_BL.find(u => u.usuario === usuario && u.clave === clave);
    if (!encontrado) return null;

    // Copia segura sin exponer la contraseña
    const { clave: _, ...perfilSeguro } = encontrado;
    return perfilSeguro;
}

/**
 * Compatibilidad con la función previa de inicio de sesión
 */
function blIniciarSesion(usuario, clave) {
    return blAutenticar(usuario, clave) !== null;
}

/**
 * Guarda la sesión activa en sessionStorage
 * @param {Object} usuarioObj - Datos del perfil de usuario autenticado
 */
function blGuardarSesion(usuarioObj) {
    sessionStorage.setItem(CLAVE_AUTENTICACION_BL, 'true');
    sessionStorage.setItem(CLAVE_USUARIO_ACTIVO_BL, JSON.stringify(usuarioObj));
}

/**
 * Retorna el usuario activo de la sesión actual.
 * @returns {Object|null}
 */
function blObtenerUsuarioActual() {
    try {
        const datos = sessionStorage.getItem(CLAVE_USUARIO_ACTIVO_BL);
        if (datos) return JSON.parse(datos);
    } catch (e) {
        console.warn('Error al leer usuario activo:', e);
    }
    return null;
}

/**
 * Actualiza parcialmente los datos del usuario en la sesión activa (por ejemplo, saldo).
 * @param {Object} camposActualizados
 */
function blActualizarUsuarioActual(camposActualizados) {
    const actual = blObtenerUsuarioActual();
    if (!actual) return;
    const nuevo = { ...actual, ...camposActualizados };
    sessionStorage.setItem(CLAVE_USUARIO_ACTIVO_BL, JSON.stringify(nuevo));
}

/**
 * Retorna el rol del usuario autenticado ('ADMIN', 'USER' o null).
 * @returns {string|null}
 */
function blObtenerRol() {
    const u = blObtenerUsuarioActual();
    return u ? u.rol : null;
}

/**
 * Verifica si hay una sesión activa y si el rol del usuario está permitido.
 * Debe ejecutarse en el <head> de cada vista para proteger el acceso.
 *
 * @param {string[]} [rolesPermitidos] - Lista de roles autorizados (ej: ['ADMIN'], ['USER'])
 */
function blVerificarAutenticacion(rolesPermitidos = null) {
    const estaAutenticado = sessionStorage.getItem(CLAVE_AUTENTICACION_BL) === 'true';
    if (!estaAutenticado) {
        window.location.replace('login.html');
        return;
    }

    if (rolesPermitidos && Array.isArray(rolesPermitidos) && rolesPermitidos.length > 0) {
        const rol = blObtenerRol();
        if (!rolesPermitidos.includes(rol)) {
            // Redirigir al área correspondiente según su rol
            if (rol === 'USER') {
                window.location.replace('transferencias.html');
            } else if (rol === 'ADMIN') {
                window.location.replace('index.html');
            } else {
                window.location.replace('login.html');
            }
        }
    }
}

/**
 * Cierra la sesión activa y redirige al inicio de sesión.
 */
function blCerrarSesion() {
    sessionStorage.removeItem(CLAVE_AUTENTICACION_BL);
    sessionStorage.removeItem(CLAVE_USUARIO_ACTIVO_BL);
    window.location.replace('login.html');
}

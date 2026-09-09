/**
 * ==========================================================================
 * LAMBDA SHIELD · transferencias.js
 * --------------------------------------------------------------------------
 * Controlador del flujo y vista del Usuario Estándar (USER).
 * - Maneja el formulario de transferencias bancarias según modelos del sistema.
 * - Conecta con MotorFraude para evaluar el riesgo en tiempo real.
 * - Despliega notificaciones visuales: Aprobada, Verificación Manual, Rechazada.
 * - Persiste en localStorage (estadoBancoLambda) para sincronizar con el Admin.
 * ==========================================================================
 */

(() => {
    'use strict';

    // Referencias DOM
    const $ = (sel) => document.querySelector(sel);

    const form = $('#formTransferencia');
    const radioFrecuente = $('#beneficiarioFrecuente');
    const radioNuevo = $('#beneficiarioNuevo');
    const secFrecuente = $('#seccionBeneficiarioFrecuente');
    const secNuevo = $('#seccionBeneficiarioNuevo');
    const selectFrecuente = $('#selectContactoFrecuente');
    const inputNuevoNombre = $('#nuevoNombreBeneficiario');
    const inputNuevoCuenta = $('#nuevoNumeroCuenta');
    const selectNuevoBanco = $('#nuevoBanco');
    const inputMonto = $('#montoTransferencia');
    const selectCategoria = $('#categoriaTransferencia');
    const inputConcepto = $('#conceptoTransferencia');

    // Contexto de seguridad
    const selectDispositivo = $('#trDispositivo');
    const selectIp = $('#trIp');
    const selectPais = $('#trPais');
    const inputVelocidad = $('#trVelocidad');

    // Paneles de notificación
    const notifInicial = $('#notificacionInicial');
    const notifResultado = $('#notificacionResultado');
    const cajaVeredictoVisual = $('#cajaVeredictoVisual');
    const iconoVeredicto = $('#iconoVeredicto');
    const tituloVeredicto = $('#tituloVeredicto');
    const mensajeVeredicto = $('#mensajeVeredicto');
    const resCodigo = $('#resCodigo');
    const resBeneficiario = $('#resBeneficiario');
    const resMonto = $('#resMonto');
    const resBadgeRiesgo = $('#resBadgeRiesgo');
    const resPuntaje = $('#resPuntaje');
    const textoMotivoVeredicto = $('#textoMotivoVeredicto');

    // Tabla de transferencias
    const tablaMisTransferencias = $('#tablaMisTransferencias');
    const btnCerrarSesion = $('#btnCerrarSesion');

    // Instancia del motor antifraude existente
    const motor = new MotorFraude();

    // Obtener usuario activo de la sesión
    let usuarioActual = blObtenerUsuarioActual() || {
        usuario: 'usuario',
        rol: 'USER',
        nombre: 'Mirko Ruiz',
        clienteId: 'C-001',
        numeroCuenta: '191-4829103-0-45',
        tipoCuenta: 'Cuenta Sueldo Ahorros (Soles)',
        saldo: 241800.50,
        promedioCliente: 180
    };

    /* ---------------------------------------------------------
       Gestión de Persistencia Compartida (estadoBancoLambda)
       --------------------------------------------------------- */
    function cargarEstadoGlobal() {
        try {
            const guardado = localStorage.getItem('estadoBancoLambda');
            if (guardado) {
                const parsed = JSON.parse(guardado);
                parsed.transacciones.forEach(t => t.fecha = new Date(t.fecha));
                return parsed;
            }
        } catch (e) {
            console.warn('Error al leer estado global:', e);
        }
        return {
            transacciones: [],
            siguienteId: 1,
            filtroActivo: 'todas',
            intervaloAutoId: null
        };
    }

    function guardarEstadoGlobal(estado) {
        try {
            localStorage.setItem('estadoBancoLambda', JSON.stringify({
                transacciones: estado.transacciones,
                siguienteId: estado.siguienteId
            }));
        } catch (e) {
            console.warn('Error al guardar estado global:', e);
        }
    }

    /* ---------------------------------------------------------
       Inicialización de Perfil y Saldo en Pantalla
       --------------------------------------------------------- */
    function renderizarPerfilUsuario() {
        if ($('#nombreUsuarioNav')) $('#nombreUsuarioNav').textContent = usuarioActual.nombre;
        if ($('#nombreTitularCuenta')) $('#nombreTitularCuenta').textContent = usuarioActual.nombre;
        if ($('#numeroCuentaTitular')) $('#numeroCuentaTitular').textContent = usuarioActual.numeroCuenta;
        if ($('#tipoCuentaTitular')) $('#tipoCuentaTitular').textContent = usuarioActual.tipoCuenta || 'Cuenta Sueldo Ahorros';
        
        actualizarDisplaySaldo();
    }

    function actualizarDisplaySaldo() {
        if ($('#saldoDisponibleDisplay')) {
            $('#saldoDisponibleDisplay').textContent = `S/ ${Number(usuarioActual.saldo).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        }
    }

    /* ---------------------------------------------------------
       Alternar entre Beneficiario Frecuente y Nuevo
       --------------------------------------------------------- */
    if (radioFrecuente && radioNuevo) {
        radioFrecuente.addEventListener('change', () => {
            secFrecuente.classList.remove('d-none');
            secNuevo.classList.add('d-none');
        });
        radioNuevo.addEventListener('change', () => {
            secFrecuente.classList.add('d-none');
            secNuevo.classList.remove('d-none');
        });
    }

    /* ---------------------------------------------------------
       Construir Datos de la Transferencia
       --------------------------------------------------------- */
    function obtenerDatosTransferencia() {
        const esNuevo = radioNuevo.checked;
        let nombreBeneficiario = '';
        let cuentaDestino = '';
        let bancoDestino = '';

        if (esNuevo) {
            nombreBeneficiario = inputNuevoNombre.value.trim() || 'Nuevo Destinatario';
            cuentaDestino = inputNuevoCuenta.value.trim() || '002191000000000000';
            bancoDestino = selectNuevoBanco.value;
        } else {
            const selectedOpt = selectFrecuente.options[selectFrecuente.selectedIndex];
            nombreBeneficiario = selectedOpt.value;
            cuentaDestino = selectedOpt.dataset.cuenta || '191-4567891-0-23';
            bancoDestino = selectedOpt.dataset.banco || 'BCP';
        }

        const monto = parseFloat(inputMonto.value) || 0;
        const horaActual = new Date().getHours();

        return {
            cliente: usuarioActual.clienteId || 'C-001',
            clienteNombre: usuarioActual.nombre || 'María Fernández',
            monto: monto,
            categoria: selectCategoria.value,
            hora: horaActual,
            pais: selectPais ? selectPais.value : 'local',
            dispositivo: selectDispositivo ? selectDispositivo.value : 'reconocido',
            operacionesUltimaHora: selectDispositivo ? (parseInt(inputVelocidad.value, 10) || 1) : 1,
            beneficiario: esNuevo ? 'nuevo' : 'recurrente',
            ip: selectIp ? selectIp.value : 'conocida',
            // Atributos de detalle de la transferencia
            beneficiarioNombre: nombreBeneficiario,
            cuentaDestino: cuentaDestino,
            bancoDestino: bancoDestino,
            concepto: inputConcepto.value.trim() || 'Transferencia inmediata'
        };
    }

    /* ---------------------------------------------------------
       Mostrar Notificación Visual de Resultado de Evaluación
       --------------------------------------------------------- */
    function mostrarNotificacionVisual(registro) {
        if (!notifInicial || !notifResultado) return;

        notifInicial.classList.add('d-none');
        notifResultado.classList.remove('d-none');

        const { veredicto, tr } = registro;
        const puntaje = veredicto.puntaje;
        const nivel = veredicto.nivel; // 'bajo', 'medio', 'alto', 'critico'

        // Limpiar clases previas
        cajaVeredictoVisual.className = 'p-4 rounded-3 text-center mb-4 border';
        iconoVeredicto.className = 'bi fs-1 d-block mb-2';

        // 1. Aprobada / Aceptada (Riesgo Bajo o Medio < 60)
        if (nivel === 'bajo' || nivel === 'medio') {
            cajaVeredictoVisual.style.backgroundColor = 'rgba(20, 108, 46, 0.08)';
            cajaVeredictoVisual.style.borderColor = 'var(--riesgo-bajo)';
            cajaVeredictoVisual.style.color = 'var(--riesgo-bajo)';

            iconoVeredicto.className = 'bi bi-check-circle-fill fs-1 d-block mb-2 text-success';
            tituloVeredicto.textContent = 'Transferencia Aprobada Exitosamente';
            tituloVeredicto.className = 'fw-bold mb-1 text-success';
            mensajeVeredicto.textContent = 'La transacción fue validada en tiempo real. No se detectaron anomalías y los fondos han sido enviados.';

            resBadgeRiesgo.textContent = nivel === 'bajo' ? 'Riesgo Bajo (Aprobada)' : 'Riesgo Medio (Aprobada)';
            resBadgeRiesgo.style.backgroundColor = 'rgba(20, 108, 46, 0.15)';
            resBadgeRiesgo.style.color = 'var(--riesgo-bajo)';
            resBadgeRiesgo.style.border = '1px solid var(--riesgo-bajo)';
        }
        // 2. En Proceso de Verificación Manual (Riesgo Moderado / Alto 60 - 79)
        else if (nivel === 'alto') {
            cajaVeredictoVisual.style.backgroundColor = 'rgba(194, 65, 12, 0.08)';
            cajaVeredictoVisual.style.borderColor = 'var(--riesgo-alto)';
            cajaVeredictoVisual.style.color = 'var(--riesgo-alto)';

            iconoVeredicto.className = 'bi bi-hourglass-split fs-1 d-block mb-2';
            iconoVeredicto.style.color = 'var(--riesgo-alto)';
            tituloVeredicto.textContent = 'En Proceso de Verificación Manual';
            tituloVeredicto.className = 'fw-bold mb-1';
            tituloVeredicto.style.color = 'var(--riesgo-alto)';
            mensajeVeredicto.textContent = 'Se identificaron señales inusuales (riesgo moderado). Tu operación ha sido retenida temporalmente para validación por un analista de seguridad.';

            resBadgeRiesgo.textContent = 'Riesgo Alto (Verificación Manual)';
            resBadgeRiesgo.style.backgroundColor = 'rgba(194, 65, 12, 0.15)';
            resBadgeRiesgo.style.color = 'var(--riesgo-alto)';
            resBadgeRiesgo.style.border = '1px solid var(--riesgo-alto)';
        }
        // 3. Rechazada por Alto Riesgo (Riesgo Crítico >= 80)
        else {
            cajaVeredictoVisual.style.backgroundColor = 'rgba(200, 16, 46, 0.08)';
            cajaVeredictoVisual.style.borderColor = 'var(--riesgo-critico)';
            cajaVeredictoVisual.style.color = 'var(--riesgo-critico)';

            iconoVeredicto.className = 'bi bi-x-octagon-fill fs-1 d-block mb-2 text-danger';
            tituloVeredicto.textContent = 'Transferencia Rechazada por Alto Riesgo';
            tituloVeredicto.className = 'fw-bold mb-1 text-danger';
            mensajeVeredicto.textContent = 'Operación bloqueada preventivamente. La transacción superó el umbral crítico de riesgo de las políticas antifraude de la entidad.';

            resBadgeRiesgo.textContent = 'Riesgo Crítico (Bloqueada)';
            resBadgeRiesgo.style.backgroundColor = 'rgba(200, 16, 46, 0.15)';
            resBadgeRiesgo.style.color = 'var(--riesgo-critico)';
            resBadgeRiesgo.style.border = '1px solid var(--riesgo-critico)';
        }

        // Datos del comprobante
        resCodigo.textContent = registro.id;
        resBeneficiario.textContent = `${tr.beneficiarioNombre} (${tr.bancoDestino} - ${tr.cuentaDestino})`;
        resMonto.textContent = `S/ ${tr.monto.toFixed(2)}`;
        resPuntaje.textContent = `${puntaje} / 100`;

        // Identificar factores más influyentes
        const factoresRiesgo = veredicto.factores
            .filter(f => f.puntaje > 0)
            .sort((a, b) => b.puntaje - a.puntaje);

        if (factoresRiesgo.length > 0) {
            const motivos = factoresRiesgo.slice(0, 2).map(f => f.motivo).join(' | ');
            textoMotivoVeredicto.textContent = `Factores evaluados: ${motivos}`;
        } else {
            textoMotivoVeredicto.textContent = 'Comportamiento habitual dentro de todos los parámetros de seguridad permitidos.';
        }
    }

    /* ---------------------------------------------------------
       Renderizar Historial de Transferencias del Usuario
       --------------------------------------------------------- */
    function renderizarHistorialUsuario() {
        if (!tablaMisTransferencias) return;
        const estadoGlobal = cargarEstadoGlobal();

        // Filtrar transacciones pertenecientes al cliente logueado
        const misTransacciones = estadoGlobal.transacciones.filter(
            t => t.tr.cliente === (usuarioActual.clienteId || 'C-001')
        );

        tablaMisTransferencias.innerHTML = '';

        if (misTransacciones.length === 0) {
            tablaMisTransferencias.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-secondary py-4">
                        No has realizado transferencias aún en esta sesión.
                    </td>
                </tr>`;
            return;
        }

        const diccRubros = {
            retail: 'Retail / Compras',
            servicios: 'Servicios',
            restaurante: 'Restaurantes',
            electronica: 'Tecnología',
            casino: 'Casino / Apuestas',
            cripto: 'Criptomonedas'
        };

        misTransacciones.slice(0, 10).forEach(item => {
            const fecha = new Date(item.fecha);
            const fechaStr = `${fecha.toLocaleDateString('es-PE')} ${fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`;
            const nivel = item.veredicto.nivel;

            let badgeEstado = '';
            if (nivel === 'bajo' || nivel === 'medio') {
                badgeEstado = `<span class="badge" style="background-color: rgba(20, 108, 46, 0.12); color: var(--riesgo-bajo); border: 1px solid var(--riesgo-bajo);"><i class="bi bi-check-circle-fill me-1"></i> Aprobada</span>`;
            } else if (nivel === 'alto') {
                badgeEstado = `<span class="badge" style="background-color: rgba(194, 65, 12, 0.12); color: var(--riesgo-alto); border: 1px solid var(--riesgo-alto);"><i class="bi bi-hourglass-split me-1"></i> En Verificación</span>`;
            } else {
                badgeEstado = `<span class="badge" style="background-color: rgba(200, 16, 46, 0.12); color: var(--riesgo-critico); border: 1px solid var(--riesgo-critico);"><i class="bi bi-x-octagon-fill me-1"></i> Rechazada</span>`;
            }

            const fila = document.createElement('tr');
            fila.innerHTML = `
                <td class="dato-mono fw-bold small">${item.id}</td>
                <td class="small text-secondary">${fechaStr}</td>
                <td class="fw-semibold small">${item.tr.beneficiarioNombre || item.tr.clienteNombre}</td>
                <td class="small">${diccRubros[item.tr.categoria] || item.tr.categoria}</td>
                <td class="dato-mono fw-bold">S/ ${item.tr.monto.toFixed(2)}</td>
                <td>${badgeEstado}</td>
            `;
            tablaMisTransferencias.appendChild(fila);
        });
    }

    /* ---------------------------------------------------------
       Procesamiento del Formulario de Transferencia
       --------------------------------------------------------- */
    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();

            const datosTr = obtenerDatosTransferencia();

            if (datosTr.monto <= 0) {
                alert('Ingresa un monto válido mayor a cero.');
                return;
            }

            // Verificar si tiene saldo suficiente
            if (datosTr.monto > usuarioActual.saldo) {
                alert(`Saldo insuficiente. Tu saldo disponible es de S/ ${usuarioActual.saldo.toFixed(2)}`);
                return;
            }

            // 1. Evaluar riesgo con MotorFraude reutilizando las 8 reglas y umbrales existentes
            const promedioCliente = usuarioActual.promedioCliente || 180;
            const veredicto = motor.evaluar(datosTr, { promedioCliente });

            // 2. Crear registro oficial
            const estadoGlobal = cargarEstadoGlobal();
            const idRegistro = `TR-${String(estadoGlobal.siguienteId++).padStart(5, '0')}`;

            const nuevoRegistro = {
                id: idRegistro,
                fecha: new Date(),
                tr: datosTr,
                veredicto: veredicto
            };

            // 3. Si la transacción es Aprobada (Bajo o Medio), descontar del saldo
            if (veredicto.nivel === 'bajo' || veredicto.nivel === 'medio') {
                usuarioActual.saldo -= datosTr.monto;
                blActualizarUsuarioActual({ saldo: usuarioActual.saldo });
                actualizarDisplaySaldo();
            }

            // 4. Guardar en estado global de localStorage para el Administrador
            estadoGlobal.transacciones.unshift(nuevoRegistro);
            guardarEstadoGlobal(estadoGlobal);

            // 5. Mostrar la notificación visual del resultado
            mostrarNotificacionVisual(nuevoRegistro);

            // 6. Actualizar la tabla de historial
            renderizarHistorialUsuario();

            // Scroll suave hacia la notificación
            $('#contenedorNotificacion').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
    }

    /* ---------------------------------------------------------
       Cerrar Sesión
       --------------------------------------------------------- */
    if (btnCerrarSesion) {
        btnCerrarSesion.addEventListener('click', blCerrarSesion);
    }

    /* ---------------------------------------------------------
       Arranque inicial
       --------------------------------------------------------- */
    renderizarPerfilUsuario();
    renderizarHistorialUsuario();

})();

/**
 * ==========================================================================
 * LAMBDA SHIELD · app.js
 * --------------------------------------------------------------------------
 * Capa de interfaz: conecta la lógica pura de motorFraude.js con el DOM.
 * Incluye manejo seguro de localStorage para la arquitectura multipágina
 * y renderizado condicional de elementos (Radar SVG, Tabla, KPIs).
 * ==========================================================================
 */

(() => {
    'use strict';

    /* ---------------------------------------------------------
       Estado de la aplicación (Persistencia segura)
       --------------------------------------------------------- */
    function cargarEstado() {
        try {
            const guardado = localStorage.getItem('estadoBancoLambda');
            if (guardado) {
                const parseado = JSON.parse(guardado);
                parseado.transacciones.forEach(t => t.fecha = new Date(t.fecha));
                
                // CORRECCIÓN: Garantizar que al cargar memoria, el filtro inicie en 'todas'
                parseado.filtroActivo = 'todas';
                parseado.intervaloAutoId = null;
                
                return parseado;
            }
        } catch (error) {
            console.warn("Error al cargar el historial:", error);
        }
        return {
            transacciones: [],
            siguienteId: 1,
            filtroActivo: 'todas',
            intervaloAutoId: null
        };
    }

    const estado = cargarEstado();
    const motor = new MotorFraude();

    function guardarEstado() {
        try {
            localStorage.setItem('estadoBancoLambda', JSON.stringify({
                transacciones: estado.transacciones,
                siguienteId: estado.siguienteId
            }));
        } catch (error) {
            console.warn("Error al guardar el historial:", error);
        }
    }

    const NIVEL_META = {
        bajo:     { etiqueta: 'Bajo',     color: 'var(--riesgo-bajo)' },
        medio:    { etiqueta: 'Medio',    color: 'var(--riesgo-medio)' },
        alto:     { etiqueta: 'Alto',     color: 'var(--riesgo-alto)' },
        critico:  { etiqueta: 'Crítico',  color: 'var(--riesgo-critico)' },
    };

    const ESTADO_POR_NIVEL = {
        bajo:    { texto: 'Aprobada',                icono: 'bi-check-circle-fill',       color: 'var(--riesgo-bajo)' },
        medio:   { texto: 'Monitoreo',               icono: 'bi-eye-fill',                color: 'var(--riesgo-medio)' },
        alto:    { texto: 'Revisión manual',         icono: 'bi-flag-fill',               color: 'var(--riesgo-alto)' },
        critico: { texto: 'Bloqueada',               icono: 'bi-x-octagon-fill',          color: 'var(--riesgo-critico)' },
    };

    /* ---------------------------------------------------------
       Referencias al DOM (Validadas para múltiples páginas)
       --------------------------------------------------------- */
    const $ = (sel) => document.querySelector(sel);

    const form             = $('#formTransaccion');
    const cuerpoTabla      = $('#tablaTransacciones');
    const resultadoVacio   = $('#resultadoVacio');
    const resultadoLleno   = $('#resultadoLleno');
    const anilloPuntaje    = $('#anilloPuntaje');
    const numeroPuntaje    = $('#numeroPuntaje');
    const insigniaNivel    = $('#insigniaNivel');
    const textoVeredicto   = $('#textoVeredicto');
    const contenedorRadar  = $('#contenedorRadar');
    const listaFactores    = $('#listaFactores');

    /* ---------------------------------------------------------
       Helpers de dominio
       --------------------------------------------------------- */
    function obtenerNombreCliente(codigoCliente) {
        if (!form) return codigoCliente;
        const opt = form.querySelector(`#trCliente option[value="${codigoCliente}"]`);
        return opt ? opt.textContent.split('·')[0].trim() : codigoCliente;
    }

    function formatearEtiqueta(tipo, codigo) {
        const diccionarios = {
            pais: { local: 'Perú', regional: 'Región', alto_riesgo: 'Alto Riesgo' },
            categoria: { retail: 'Retail', restaurante: 'Restaurante', servicios: 'Servicios', electronica: 'Electrónica', casino: 'Casino', cripto: 'Cripto' },
            beneficiario: { recurrente: 'Conocido', nuevo: 'Nuevo' },
            ip: { conocida: 'Habitual', nueva: 'Nueva' }
        };
        return diccionarios[tipo][codigo] || codigo;
    }

    function leerTransaccionFormulario() {
        const cliente = $('#trCliente').value;
        const [hh] = $('#trHora').value.split(':');
        return {
            cliente,
            clienteNombre: obtenerNombreCliente(cliente),
            monto: Number($('#trMonto').value) || 0,
            categoria: $('#trCategoria').value,
            hora: Number(hh),
            pais: $('#trPais').value,
            dispositivo: $('#trDispositivo').value,
            operacionesUltimaHora: Number($('#trVelocidad').value) || 1,
            beneficiario: $('#trBeneficiario').value,
            ip: $('#trIp').value,
        };
    }

    /* ---------------------------------------------------------
       Evaluación de transacción principal
       --------------------------------------------------------- */
    function evaluarYRegistrar(tr) {
        const optPromedio = form ? form.querySelector(`#trCliente option[value="${tr.cliente}"]`) : null;
        const promedioCliente = optPromedio ? Number(optPromedio.dataset.promedio) : 100;
        
        const veredicto = motor.evaluar(tr, { promedioCliente });

        const registro = {
            id: `TR-${String(estado.siguienteId++).padStart(5, '0')}`,
            fecha: new Date(),
            tr,
            veredicto,
        };

        // Insertar al inicio del array para que el más reciente sea el primero
        estado.transacciones.unshift(registro);
        guardarEstado();

        // Actualizar UI condicionalmente según la página actual
        if (form) pintarResultadoPrincipal(registro);
        if (cuerpoTabla) pintarFilaTabla(registro, true);
        actualizarKPIs();

        if (veredicto.nivel === 'critico') lanzarAlerta(registro);
        
        return registro;
    }

    /* ---------------------------------------------------------
       Tabla de transacciones (Historial Detallado)
       --------------------------------------------------------- */
    function repintarTablaCompleta() {
        if (!cuerpoTabla) return;
        cuerpoTabla.innerHTML = '';
        
        if (estado.transacciones.length === 0) {
            cuerpoTabla.innerHTML = `<tr class="tr-vacia"><td colspan="9" class="text-center text-secondary py-5">No hay transacciones evaluadas en el historial.</td></tr>`;
            return;
        }
        
        // Iterar y agregar al final (el array ya está ordenado del más nuevo al más viejo)
        estado.transacciones.forEach(r => pintarFilaTabla(r, false));
        reaplicarFiltro();
    }

    function pintarFilaTabla(registro, esNueva) {
        if (!cuerpoTabla) return;

        // Limpiar el mensaje de "vacío" si existe
        const vacia = cuerpoTabla.querySelector('.tr-vacia');
        if (vacia) vacia.remove();

        const { tr, veredicto } = registro;
        const meta = NIVEL_META[veredicto.nivel];
        const desc = ESTADO_POR_NIVEL[veredicto.nivel];

        const fila = document.createElement('tr');
        fila.dataset.nivel = veredicto.nivel;
        
        // Animación visual si es una transacción recién ingresada
        if (esNueva) {
            fila.style.backgroundColor = 'var(--fondo-input)';
            setTimeout(() => fila.style.backgroundColor = 'transparent', 1200);
        }
        
        fila.style.display = coincideFiltro(veredicto.nivel) ? '' : 'none';

        fila.innerHTML = `
            <td class="dato-mono fw-bold">${registro.id}</td>
            <td>${tr.clienteNombre}</td>
            <td class="dato-mono">S/ ${tr.monto.toFixed(2)}</td>
            <td>${formatearEtiqueta('categoria', tr.categoria)}</td>
            <td>${formatearEtiqueta('pais', tr.pais)}</td>
            <td class="dato-mono">${String(tr.hora).padStart(2, '0')}:00</td>
            <td class="dato-mono fw-bold" style="color: ${meta.color};">${veredicto.puntaje}</td>
            <td><span class="etiqueta-nivel" style="background-color:${meta.color}22; color:${meta.color}; border: 1px solid ${meta.color};">${meta.etiqueta}</span></td>
            <td><span style="color:${desc.color}; font-size:0.8rem; font-weight:600;"><i class="bi ${desc.icono}"></i> ${desc.texto}</span></td>
        `;

        if (esNueva) {
            cuerpoTabla.prepend(fila);
        } else {
            cuerpoTabla.appendChild(fila);
        }
    }

    function coincideFiltro(nivel) {
        return estado.filtroActivo === 'todas' || estado.filtroActivo === nivel;
    }

    function reaplicarFiltro() {
        if (!cuerpoTabla) return;
        cuerpoTabla.querySelectorAll('tr[data-nivel]').forEach(tr => {
            tr.style.display = coincideFiltro(tr.dataset.nivel) ? '' : 'none';
        });
    }

    /* ---------------------------------------------------------
       Panel "Veredicto del motor" (Simulador)
       --------------------------------------------------------- */
    function pintarResultadoPrincipal(registro) {
        if (!resultadoVacio || !resultadoLleno) return;
        
        resultadoVacio.classList.add('d-none');
        resultadoLleno.classList.remove('d-none');

        const { puntaje, nivel, factores } = registro.veredicto;
        const meta = NIVEL_META[nivel];

        anilloPuntaje.style.setProperty('--p', `${(puntaje / 100) * 360}deg`);
        anilloPuntaje.style.setProperty('--c', meta.color);
        numeroPuntaje.textContent = puntaje;

        insigniaNivel.textContent = `Riesgo ${meta.etiqueta}`;
        insigniaNivel.style.backgroundColor = `${meta.color}22`;
        insigniaNivel.style.color = meta.color;
        insigniaNivel.style.border = `1px solid ${meta.color}`;

        const desc = ESTADO_POR_NIVEL[nivel];
        textoVeredicto.innerHTML = `<i class="bi ${desc.icono}" style="color:${desc.color}"></i> ${desc.texto} · ${registro.id}`;

        contenedorRadar.innerHTML = construirRadarSVG(factores);
        listaFactores.innerHTML = factores.map(construirFactorHTML).join('');
    }

    function construirFactorHTML(f) {
        const porcentaje = Math.round((f.puntaje / f.max) * 100);
        let colorBarra = 'var(--riesgo-bajo)';
        if (porcentaje >= 45) colorBarra = 'var(--riesgo-alto)';
        if (porcentaje >= 75) colorBarra = 'var(--riesgo-critico)';

        return `
            <div class="col-md-6">
                <div class="item-factor">
                    <div class="d-flex justify-content-between mb-1" style="font-size: 0.85rem; color: var(--texto-principal);">
                        <span>${f.etiqueta}</span>
                        <strong class="dato-mono">${f.puntaje}/${f.max}</strong>
                    </div>
                    <div class="barra-factor"><span style="width:${porcentaje}%; background:${colorBarra}"></span></div>
                    <div class="text-secondary mt-1" style="font-size: 0.75rem; line-height: 1.3;">${f.motivo}</div>
                </div>
            </div>`;
    }

    /* ---------------------------------------------------------
       Radar SVG (Estilo oscuro / rojo UTP)
       --------------------------------------------------------- */
    function construirRadarSVG(factores) {
        const size = 240, center = size / 2, radius = 84, n = factores.length;
        const puntoEnEje = (i, ratio) => {
            const angulo = (Math.PI * 2 * i) / n - Math.PI / 2;
            return [center + Math.cos(angulo) * radius * ratio, center + Math.sin(angulo) * radius * ratio];
        };

        const anillos = [0.25, 0.5, 0.75, 1].map(r => {
            const pts = factores.map((_, i) => puntoEnEje(i, r).join(',')).join(' ');
            return `<polygon points="${pts}" fill="none" stroke="var(--borde-oscuro)" stroke-width="1"/>`;
        }).join('');

        const ejes = factores.map((_, i) => {
            const [x, y] = puntoEnEje(i, 1);
            return `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="var(--borde-oscuro)" stroke-width="1"/>`;
        }).join('');

        const datoPts = factores.map((f, i) => puntoEnEje(i, f.puntaje / f.max).join(',')).join(' ');

        const etiquetasCortas = { monto: 'Monto', ubicacion: 'Ubic.', horario: 'Horario', velocidad: 'Veloc.', dispositivo: 'Disp.', categoria: 'Rubro', beneficiario: 'Benef.', ip: 'IP' };
        
        const labels = factores.map((f, i) => {
            const [x, y] = puntoEnEje(i, 1.22);
            return `<text x="${x}" y="${y}" text-anchor="middle" dominant-baseline="middle" font-size="10" fill="var(--texto-secundario)" font-family="system-ui">${etiquetasCortas[f.id] || f.etiqueta}</text>`;
        }).join('');

        return `
        <svg viewBox="0 0 ${size} ${size}" width="100%" height="240">
            ${anillos}
            ${ejes}
            <polygon points="${datoPts}" fill="var(--utp-rojo-brillo)" stroke="var(--utp-rojo)" stroke-width="2"/>
            ${labels}
        </svg>`;
    }

    /* ---------------------------------------------------------
       KPIs y Alertas
       --------------------------------------------------------- */
    function actualizarKPIs() {
        const total = estado.transacciones.length;
        const criticas = estado.transacciones.filter(r => r.veredicto.nivel === 'critico').length;
        const revision = estado.transacciones.filter(r => r.veredicto.nivel === 'alto').length;
        const promedio = total ? estado.transacciones.reduce((s, r) => s + r.veredicto.puntaje, 0) / total : 0;

        if ($('#kpiTotal')) $('#kpiTotal').textContent = total;
        if ($('#kpiCritico')) $('#kpiCritico').textContent = criticas;
        if ($('#kpiRevision')) $('#kpiRevision').textContent = revision;
        if ($('#kpiPuntaje')) $('#kpiPuntaje').textContent = promedio.toFixed(1);
    }

    function lanzarAlerta(registro) {
        let contenedor = document.querySelector('.contenedor-alertas');
        if (!contenedor) {
            contenedor = document.createElement('div');
            contenedor.className = 'contenedor-alertas position-fixed';
            document.body.appendChild(contenedor);
        }

        const div = document.createElement('div');
        div.className = 'alerta-ls';
        div.innerHTML = `
            <strong class="d-block mb-1 text-danger" style="font-size: 0.9rem;"><i class="bi bi-shield-x"></i> Transacción Bloqueada</strong>
            <span class="text-secondary small">${registro.id} · ${registro.tr.clienteNombre} · S/ ${registro.tr.monto.toFixed(2)} · Score: ${registro.veredicto.puntaje}</span>
        `;
        contenedor.appendChild(div);
        
        setTimeout(() => {
            div.style.opacity = '0';
            div.style.transition = 'opacity 0.4s ease';
            setTimeout(() => div.remove(), 400);
        }, 5000);
    }

    /* ---------------------------------------------------------
       Generador de tráfico (Simulación)
       --------------------------------------------------------- */
    function generarTransaccionAleatoria() {
        const clientes = ['C-001', 'C-002', 'C-003'];
        const categorias = ['retail', 'restaurante', 'servicios', 'electronica', 'casino', 'cripto'];
        const paises = ['local', 'local', 'local', 'regional', 'alto_riesgo'];
        
        const cliente = clientes[Math.floor(Math.random() * clientes.length)];
        const optPromedio = form ? form.querySelector(`#trCliente option[value="${cliente}"]`) : null;
        const promedio = optPromedio ? Number(optPromedio.dataset.promedio) : 100;

        const esSospechosa = Math.random() < 0.25;

        return {
            cliente,
            clienteNombre: cliente === 'C-001' ? 'María Fernández' : cliente === 'C-002' ? 'Jorge Salas' : 'Ana Quiroz',
            monto: esSospechosa ? Math.round(promedio * (4 + Math.random() * 6)) : Math.round(promedio * (0.5 + Math.random() * 1.2)),
            categoria: esSospechosa ? categorias[Math.floor(Math.random() * categorias.length)] : categorias[Math.floor(Math.random() * 4)],
            hora: esSospechosa ? Math.floor(Math.random() * 5) : Math.floor(Math.random() * 24),
            pais: esSospechosa ? paises[3 + Math.floor(Math.random() * 2)] : paises[Math.floor(Math.random() * 3)],
            dispositivo: esSospechosa ? 'nuevo' : 'reconocido',
            operacionesUltimaHora: esSospechosa ? 4 + Math.floor(Math.random() * 8) : 1 + Math.floor(Math.random() * 2),
            beneficiario: esSospechosa ? 'nuevo' : 'recurrente',
            ip: esSospechosa ? 'nueva' : 'conocida'
        };
    }

    function alternarTraficoAutomatico() {
        const btn = $('#btnAuto');
        if (estado.intervaloAutoId) {
            clearInterval(estado.intervaloAutoId);
            estado.intervaloAutoId = null;
            if (btn) {
                btn.innerHTML = '<i class="bi bi-lightning-charge-fill"></i> Iniciar tráfico automático';
                btn.classList.remove('btn-principal');
                btn.classList.add('btn-secundario');
            }
            return;
        }
        
        estado.intervaloAutoId = setInterval(() => {
            evaluarYRegistrar(generarTransaccionAleatoria());
        }, 1800);
        
        if (btn) {
            btn.innerHTML = '<i class="bi bi-stop-circle-fill"></i> Detener tráfico automático';
            btn.classList.remove('btn-secundario');
            btn.classList.add('btn-principal');
        }
    }

    /* ---------------------------------------------------------
       Eventos de Interfaz
       --------------------------------------------------------- */
    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            evaluarYRegistrar(leerTransaccionFormulario());
        });
    }

    document.querySelectorAll('.btn-filtro').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.btn-filtro').forEach(b => {
                b.classList.remove('btn-principal');
                b.classList.add('btn-secundario');
            });
            btn.classList.remove('btn-secundario');
            btn.classList.add('btn-principal');
            estado.filtroActivo = btn.dataset.filtro;
            reaplicarFiltro();
        });
    });

    if ($('#btnAuto')) $('#btnAuto').addEventListener('click', alternarTraficoAutomatico);

    if ($('#btnReset')) {
        $('#btnReset').addEventListener('click', () => {
            if (estado.intervaloAutoId) alternarTraficoAutomatico();
            estado.transacciones = [];
            estado.siguienteId = 1;
            guardarEstado();
            repintarTablaCompleta();
            actualizarKPIs();
            if (resultadoVacio && resultadoLleno) {
                resultadoLleno.classList.add('d-none');
                resultadoVacio.classList.remove('d-none');
            }
        });
    }

    if ($('#btnCerrarSesion')) $('#btnCerrarSesion').addEventListener('click', blCerrarSesion);

    /* ---------------------------------------------------------
       Arranque inicial
       --------------------------------------------------------- */
    repintarTablaCompleta();
    actualizarKPIs();
})();


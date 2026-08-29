/**
 * ==========================================================================
 * LAMBDA SHIELD · app.js
 * --------------------------------------------------------------------------
 * Capa de interfaz: toma la lógica pura de fraudEngine.js y la conecta
 * con el DOM (formulario, tabla de transacciones, KPIs, radar SVG, toasts).
 * ==========================================================================
 */

(() => {
    'use strict';

    /* ---------------------------------------------------------
       Estado de la aplicación (todo vive en memoria: es una demo)
       --------------------------------------------------------- */
    const state = {
        engine: new FraudEngine(),
        transacciones: [],   // historial evaluado, más reciente primero
        siguienteId: 1,
        filtroActivo: 'todas',
        autoIntervalId: null,
    };

    const NIVEL_META = {
        bajo:     { label: 'Bajo',     color: 'var(--ls-risk-bajo)',    clase: 'ls-nivel-bajo' },
        medio:    { label: 'Medio',    color: 'var(--ls-risk-medio)',   clase: 'ls-nivel-medio' },
        alto:     { label: 'Alto',     color: 'var(--ls-risk-alto)',    clase: 'ls-nivel-alto' },
        critico:  { label: 'Crítico',  color: 'var(--ls-risk-critico)', clase: 'ls-nivel-critico' },
    };

    const ESTADO_POR_NIVEL = {
        bajo:    { texto: 'Aprobada',        icono: 'bi-check-circle-fill',       color: 'var(--ls-risk-bajo)' },
        medio:   { texto: 'Aprobada, con monitoreo', icono: 'bi-eye-fill',        color: 'var(--ls-risk-medio)' },
        alto:    { texto: 'En revisión manual', icono: 'bi-flag-fill',            color: 'var(--ls-risk-alto)' },
        critico: { texto: 'Bloqueada',       icono: 'bi-x-octagon-fill',          color: 'var(--ls-risk-critico)' },
    };

    /* ---------------------------------------------------------
       Referencias al DOM
       --------------------------------------------------------- */
    const $ = (sel) => document.querySelector(sel);

    const form            = $('#formTransaccion');
    const tablaBody        = $('#tablaTransacciones');
    const resultadoEmpty   = $('#resultadoEmpty');
    const resultadoBody    = $('#resultadoBody');
    const scoreRing        = $('#scoreRing');
    const scoreNumero      = $('#scoreNumero');
    const badgeNivel       = $('#badgeNivel');
    const veredictoTexto   = $('#veredictoTexto');
    const radarWrap        = $('#radarWrap');
    const listaFactores    = $('#listaFactores');
    const toastContainer   = $('#toastContainer');

    const kpiTotal    = $('#kpiTotal');
    const kpiCritical = $('#kpiCritical');
    const kpiReview   = $('#kpiReview');
    const kpiScore    = $('#kpiScore');

    /* ---------------------------------------------------------
       Helpers de dominio
       --------------------------------------------------------- */

    // Clientes demo con su gasto promedio histórico (para la regla de monto)
    function promedioDeCliente(codigoCliente) {
        const opt = form.querySelector(`#txCliente option[value="${codigoCliente}"]`);
        return opt ? Number(opt.dataset.avg) : 100;
    }

    function nombreDeCliente(codigoCliente) {
        const opt = form.querySelector(`#txCliente option[value="${codigoCliente}"]`);
        return opt ? opt.textContent.split('·')[0].trim() : codigoCliente;
    }

    function etiquetaPais(codigo) {
        return { local: 'Perú', regional: 'Región', alto_riesgo: 'Alto riesgo' }[codigo] || codigo;
    }

    function etiquetaCategoria(codigo) {
        return {
            retail: 'Retail', restaurante: 'Restaurante', servicios: 'Servicios',
            electronica: 'Electrónica', casino: 'Casino', cripto: 'Cripto',
        }[codigo] || codigo;
    }

    function leerTransaccionDelFormulario() {
        const cliente = $('#txCliente').value;
        const [hh] = $('#txHora').value.split(':');
        return {
            cliente,
            clienteNombre: nombreDeCliente(cliente),
            monto: Number($('#txMonto').value) || 0,
            categoria: $('#txCategoria').value,
            hora: Number(hh),
            pais: $('#txPais').value,
            dispositivo: $('#txDispositivo').value,
            operacionesUltimaHora: Number($('#txVelocidad').value) || 1,
        };
    }

    /* ---------------------------------------------------------
       Evaluación de una transacción + actualización de toda la UI
       --------------------------------------------------------- */
    function evaluarYRegistrar(tx) {
        const ctx = { promedioCliente: promedioDeCliente(tx.cliente) };
        const veredicto = state.engine.evaluate(tx, ctx);

        const registro = {
            id: `TX-${String(state.siguienteId++).padStart(5, '0')}`,
            fecha: new Date(),
            tx,
            veredicto,
        };

        state.transacciones.unshift(registro);
        pintarResultadoPrincipal(registro);
        pintarFilaTabla(registro, true);
        actualizarKPIs();

        if (veredicto.nivel === 'critico') {
            lanzarToast(registro);
        }
        return registro;
    }

    /* ---------------------------------------------------------
       Panel "Veredicto del motor" (anillo + radar + factores)
       --------------------------------------------------------- */
    function pintarResultadoPrincipal(registro) {
        resultadoEmpty.classList.add('d-none');
        resultadoBody.classList.remove('d-none');

        const { score, nivel, factores } = registro.veredicto;
        const meta = NIVEL_META[nivel];

        scoreRing.style.setProperty('--p', `${(score / 100) * 360}deg`);
        scoreRing.style.setProperty('--c', meta.color);
        scoreNumero.textContent = score;

        badgeNivel.textContent = `Riesgo ${meta.label}`;
        badgeNivel.className = `ls-badge-nivel mt-3 ${meta.clase}`;

        const estado = ESTADO_POR_NIVEL[nivel];
        veredictoTexto.innerHTML = `<i class="bi ${estado.icono}" style="color:${estado.color}"></i> ${estado.texto} · ${registro.id}`;

        radarWrap.innerHTML = construirRadarSVG(factores);
        listaFactores.innerHTML = factores.map(construirFactorHTML).join('');
    }

    function construirFactorHTML(f) {
        const pct = Math.round((f.score / f.max) * 100);
        return `
            <div class="ls-factor-item">
                <div class="ls-factor-top">
                    <span>${f.label}</span>
                    <strong>${f.score}/${f.max}</strong>
                </div>
                <div class="ls-factor-bar"><span style="width:${pct}%; background:${colorPorPorcentaje(pct)}"></span></div>
                <div class="ls-factor-reason">${f.reason}</div>
            </div>`;
    }

    function colorPorPorcentaje(pct) {
        if (pct >= 75) return 'var(--ls-risk-critico)';
        if (pct >= 45) return 'var(--ls-risk-alto)';
        if (pct >= 15) return 'var(--ls-risk-medio)';
        return 'var(--ls-risk-bajo)';
    }

    /* ---------------------------------------------------------
       Radar SVG (elemento distintivo del panel):
       un eje por factor, la distancia del centro = % de riesgo
       consumido en ese factor.
       --------------------------------------------------------- */
    function construirRadarSVG(factores) {
        const size = 240;
        const center = size / 2;
        const radius = 84;
        const n = factores.length;

        const puntoEnEje = (i, ratio) => {
            const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
            return [
                center + Math.cos(angle) * radius * ratio,
                center + Math.sin(angle) * radius * ratio,
            ];
        };

        // Anillos guía (25%, 50%, 75%, 100%)
        const anillos = [0.25, 0.5, 0.75, 1].map(r => {
            const pts = factores.map((_, i) => puntoEnEje(i, r).join(',')).join(' ');
            return `<polygon points="${pts}" fill="none" stroke="#1E2A42" stroke-width="1"/>`;
        }).join('');

        // Ejes
        const ejes = factores.map((_, i) => {
            const [x, y] = puntoEnEje(i, 1);
            return `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="#1E2A42" stroke-width="1"/>`;
        }).join('');

        // Polígono de datos reales
        const datoPts = factores.map((f, i) => puntoEnEje(i, f.score / f.max).join(',')).join(' ');

        // Etiquetas cortas por eje
        const etiquetasCortas = {
            monto: 'Monto', ubicacion: 'Ubic.', horario: 'Horario',
            velocidad: 'Veloc.', dispositivo: 'Disp.', categoria: 'Rubro',
        };
        const labels = factores.map((f, i) => {
            const [x, y] = puntoEnEje(i, 1.22);
            return `<text x="${x}" y="${y}" text-anchor="middle" dominant-baseline="middle" font-size="10" fill="#8A94AC" font-family="Inter, sans-serif">${etiquetasCortas[f.id] || f.label}</text>`;
        }).join('');

        return `
        <svg viewBox="0 0 ${size} ${size}" width="100%" height="240" role="img" aria-label="Desglose de riesgo por factor">
            ${anillos}
            ${ejes}
            <polygon points="${datoPts}" fill="#19D3C530" stroke="#19D3C5" stroke-width="2"/>
            ${labels}
        </svg>`;
    }

    /* ---------------------------------------------------------
       Tabla de transacciones
       --------------------------------------------------------- */
    function pintarFilaTabla(registro, esNueva) {
        const emptyRow = tablaBody.querySelector('.ls-empty-row');
        if (emptyRow) emptyRow.remove();

        const { tx, veredicto } = registro;
        const meta = NIVEL_META[veredicto.nivel];
        const estado = ESTADO_POR_NIVEL[veredicto.nivel];

        const tr = document.createElement('tr');
        tr.dataset.nivel = veredicto.nivel;
        if (esNueva) tr.classList.add('ls-row-new');
        tr.style.display = coincideFiltro(veredicto.nivel) ? '' : 'none';

        tr.innerHTML = `
            <td class="ls-mono">${registro.id}</td>
            <td>${tx.clienteNombre}</td>
            <td class="ls-mono">S/ ${tx.monto.toFixed(2)}</td>
            <td>${etiquetaCategoria(tx.categoria)}</td>
            <td>${etiquetaPais(tx.pais)}</td>
            <td class="ls-mono">${String(tx.hora).padStart(2, '0')}:00</td>
            <td class="ls-mono">${veredicto.score}</td>
            <td><span class="ls-nivel-tag ${meta.clase}">${meta.label}</span></td>
            <td><span class="ls-estado-tag" style="color:${estado.color}"><i class="bi ${estado.icono}"></i> ${estado.texto}</span></td>
        `;
        tablaBody.prepend(tr);
    }

    function coincideFiltro(nivel) {
        return state.filtroActivo === 'todas' || state.filtroActivo === nivel;
    }

    function reaplicarFiltro() {
        tablaBody.querySelectorAll('tr[data-nivel]').forEach(tr => {
            tr.style.display = coincideFiltro(tr.dataset.nivel) ? '' : 'none';
        });
    }

    /* ---------------------------------------------------------
       KPIs agregados
       --------------------------------------------------------- */
    function actualizarKPIs() {
        const total = state.transacciones.length;
        const criticas = state.transacciones.filter(r => r.veredicto.nivel === 'critico').length;
        const revision = state.transacciones.filter(r => r.veredicto.nivel === 'alto').length;
        const promedio = total
            ? state.transacciones.reduce((s, r) => s + r.veredicto.score, 0) / total
            : 0;

        kpiTotal.textContent = total;
        kpiCritical.textContent = criticas;
        kpiReview.textContent = revision;
        kpiScore.textContent = promedio.toFixed(1);
    }

    /* ---------------------------------------------------------
       Alertas toast para transacciones críticas
       --------------------------------------------------------- */
    function lanzarToast(registro) {
        const div = document.createElement('div');
        div.className = 'ls-toast';
        div.innerHTML = `
            <strong><i class="bi bi-exclamation-octagon-fill"></i> Transacción bloqueada</strong>
            <span>${registro.id} · ${registro.tx.clienteNombre} · S/ ${registro.tx.monto.toFixed(2)} · score ${registro.veredicto.score}/100</span>
        `;
        toastContainer.appendChild(div);
        setTimeout(() => {
            div.style.opacity = '0';
            div.style.transition = 'opacity .4s ease';
            setTimeout(() => div.remove(), 400);
        }, 5000);
    }

    /* ---------------------------------------------------------
       Generador de tráfico automático (simula el "mundo real")
       --------------------------------------------------------- */
    function transaccionAleatoria() {
        const clientes = ['C-001', 'C-002', 'C-003'];
        const categorias = ['retail', 'restaurante', 'servicios', 'electronica', 'casino', 'cripto'];
        const paises = ['local', 'local', 'local', 'regional', 'alto_riesgo'];
        const dispositivos = ['reconocido', 'reconocido', 'reconocido', 'nuevo'];

        const cliente = clientes[Math.floor(Math.random() * clientes.length)];
        const promedio = promedioDeCliente(cliente);

        // 20% de probabilidad de generar un patrón claramente sospechoso,
        // para que la demo muestre casos críticos con frecuencia razonable.
        const esSospechosa = Math.random() < 0.22;

        return {
            cliente,
            clienteNombre: nombreDeCliente(cliente),
            monto: esSospechosa
                ? Math.round(promedio * (4 + Math.random() * 6))
                : Math.round(promedio * (0.5 + Math.random() * 1.2)),
            categoria: esSospechosa
                ? categorias[Math.floor(Math.random() * categorias.length)]
                : categorias[Math.floor(Math.random() * 4)], // sesga a rubros normales
            hora: esSospechosa ? Math.floor(Math.random() * 5) : Math.floor(Math.random() * 24),
            pais: esSospechosa ? paises[3 + Math.floor(Math.random() * 2)] : paises[Math.floor(Math.random() * 3)],
            dispositivo: esSospechosa ? 'nuevo' : dispositivos[Math.floor(Math.random() * dispositivos.length)],
            operacionesUltimaHora: esSospechosa ? 4 + Math.floor(Math.random() * 8) : 1 + Math.floor(Math.random() * 2),
        };
    }

    function alternarTraficoAutomatico() {
        const btn = $('#btnAuto');
        if (state.autoIntervalId) {
            clearInterval(state.autoIntervalId);
            state.autoIntervalId = null;
            btn.innerHTML = '<i class="bi bi-lightning-charge-fill"></i> Generar tráfico automático';
            btn.classList.remove('ls-btn-ghost');
            btn.classList.add('ls-btn-accent');
            return;
        }
        state.autoIntervalId = setInterval(() => {
            evaluarYRegistrar(transaccionAleatoria());
        }, 1800);
        btn.innerHTML = '<i class="bi bi-stop-circle-fill"></i> Detener tráfico automático';
        btn.classList.remove('ls-btn-accent');
        btn.classList.add('ls-btn-ghost');
    }

    /* ---------------------------------------------------------
       Panel educativo: "cómo calcula el riesgo el motor"
       --------------------------------------------------------- */
    function pintarPanelReglas() {
        const definiciones = [
            { peso: '25', titulo: 'Monto inusual', desc: 'Compara el monto contra el gasto promedio histórico del titular. Desviaciones grandes suman más puntos.' },
            { peso: '20', titulo: 'Ubicación geográfica', desc: 'Penaliza operaciones fuera del país habitual, y más aún en jurisdicciones de alto riesgo.' },
            { peso: '15', titulo: 'Horario de la operación', desc: 'La franja de madrugada (00:00–05:00) concentra estadísticamente más intentos de fraude.' },
            { peso: '20', titulo: 'Velocidad de transacciones', desc: 'Varias operaciones en poco tiempo es la firma clásica de una tarjeta comprometida.' },
            { peso: '10', titulo: 'Dispositivo / huella digital', desc: 'Un dispositivo sin historial previo incrementa la sospecha, sobre todo combinado con otras señales.' },
            { peso: '10', titulo: 'Categoría de comercio', desc: 'Rubros como casinos o exchanges de criptomonedas tienen una tasa base de fraude más alta.' },
        ];

        $('#reglasGrid').innerHTML = definiciones.map(d => `
            <div class="col-md-4">
                <div class="ls-regla-card">
                    <div class="ls-regla-peso">${d.peso} pts</div>
                    <div class="ls-regla-titulo">${d.titulo}</div>
                    <div class="ls-regla-desc">${d.desc}</div>
                </div>
            </div>
        `).join('');
    }

    /* ---------------------------------------------------------
       Reset de la demo
       --------------------------------------------------------- */
    function reiniciarDemo() {
        if (state.autoIntervalId) alternarTraficoAutomatico();
        state.transacciones = [];
        state.siguienteId = 1;
        tablaBody.innerHTML = `<tr class="ls-empty-row"><td colspan="9">Aún no hay transacciones evaluadas. Usa el simulador o genera tráfico automático.</td></tr>`;
        resultadoBody.classList.add('d-none');
        resultadoEmpty.classList.remove('d-none');
        actualizarKPIs();
    }

    /* ---------------------------------------------------------
       Eventos
       --------------------------------------------------------- */
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        evaluarYRegistrar(leerTransaccionDelFormulario());
    });

    document.querySelectorAll('.ls-btn-filter').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.ls-btn-filter').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.filtroActivo = btn.dataset.filtro;
            reaplicarFiltro();
        });
    });

    $('#btnAuto').addEventListener('click', alternarTraficoAutomatico);
    $('#btnReset').addEventListener('click', reiniciarDemo);

    /* ---------------------------------------------------------
       Arranque
       --------------------------------------------------------- */
    pintarPanelReglas();
    actualizarKPIs();

    // Pre-cargamos un par de transacciones de ejemplo para que el
    // panel no se sienta vacío al abrir la demo.
    evaluarYRegistrar({
        cliente: 'C-001', clienteNombre: 'María Fernández', monto: 165,
        categoria: 'retail', hora: 15, pais: 'local', dispositivo: 'reconocido', operacionesUltimaHora: 1,
    });
    evaluarYRegistrar({
        cliente: 'C-002', clienteNombre: 'Jorge Salas', monto: 5200,
        categoria: 'cripto', hora: 3, pais: 'alto_riesgo', dispositivo: 'nuevo', operacionesUltimaHora: 6,
    });

})();

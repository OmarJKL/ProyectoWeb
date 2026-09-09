/**
 * ==========================================================================
 * LAMBDA SHIELD · dashboard.js
 * --------------------------------------------------------------------------
 * Panel analítico para administradores (ADMIN).
 * - Sincroniza datos de transacciones desde localStorage (estadoBancoLambda).
 * - Provee dataset inicial estructurado y realista si la base está vacía.
 * - Calcula y actualiza métricas clave (KPIs) en tiempo real.
 * - Renderiza 3 gráficos analíticos con Chart.js:
 *     1. Histórico de transferencias en el tiempo (Líneas con selector de rango)
 *     2. Desglose según nivel de riesgo (Doughnut)
 *     3. Distribución por tipo de transacción / categoría (Barras)
 * - Muestra tabla de incidentes de riesgo moderado/alto y crítico.
 * ==========================================================================
 */

(() => {
    'use strict';

    // Constantes de colores corporativos Lambda Shield
    const COLOR_ROJO = '#c8102e';
    const COLOR_ROJO_OSCURO = '#9c0c23';
    const COLOR_NEGRO = '#111111';
    const COLOR_GRIS = '#6b6b6b';
    const COLOR_BORDE = '#e4e2df';

    const COLOR_RIESGO_BAJO = '#146c2e';
    const COLOR_RIESGO_MEDIO = '#b45309';
    const COLOR_RIESGO_ALTO = '#c2410c';
    const COLOR_RIESGO_CRITICO = '#9c0c23';

    let chartHistorico = null;
    let chartRiesgo = null;
    let chartCategorias = null;
    let chartHorasRiesgo = null;
    let chartBancos = null;
    let rangoDiasActivo = 30;

    /* ---------------------------------------------------------
       Carga y Generación de Datos Iniciales
       --------------------------------------------------------- */
    function obtenerEstadoTransacciones() {
        let estado = null;
        try {
            const guardado = localStorage.getItem('estadoBancoLambda');
            if (guardado) {
                estado = JSON.parse(guardado);
                estado.transacciones.forEach(t => t.fecha = new Date(t.fecha));
            }
        } catch (e) {
            console.warn('Error al leer estado en dashboard:', e);
        }

        // Si no hay datos o hay muy pocos (< 15), sembrar transacciones históricas realistas
        if (!estado || !estado.transacciones || estado.transacciones.length < 15) {
            estado = generarDatosSemilla(estado ? estado.transacciones : []);
            localStorage.setItem('estadoBancoLambda', JSON.stringify({
                transacciones: estado.transacciones,
                siguienteId: estado.siguienteId
            }));
        }

        return estado;
    }

    function generarDatosSemilla(existentes = []) {
        const clientes = [
            { id: 'C-001', nombre: 'María Fernández', prom: 180 },
            { id: 'C-002', nombre: 'Jorge Salas', prom: 950 },
            { id: 'C-003', nombre: 'Ana Quiroz', prom: 60 }
        ];

        const categorias = ['retail', 'servicios', 'restaurante', 'electronica', 'casino', 'cripto'];
        const destinatarios = ['Carlos Mendoza', 'Laura Torres', 'Distribuidora Lima SAC', 'Inversiones Pacífico', 'BetOnline SAC', 'Crypto Latam'];
        const bancos = ['BCP', 'BBVA', 'Interbank', 'Scotiabank'];

        const seedTransacciones = [...existentes];
        let siguienteId = existentes.length ? Math.max(...existentes.map(t => parseInt(t.id.replace('TR-', '')) || 0)) + 1 : 1;

        const hoy = new Date();

        // Generar 40 transacciones distribuidas en los últimos 45 días
        for (let i = 45; i >= 1; i--) {
            const cant = Math.random() < 0.3 ? 2 : 1; // 1 o 2 por día
            for (let k = 0; k < cant; k++) {
                const fecha = new Date(hoy);
                fecha.setDate(hoy.getDate() - i);
                fecha.setHours(Math.floor(Math.random() * 24), Math.floor(Math.random() * 60));

                const cli = clientes[Math.floor(Math.random() * clientes.length)];
                const cat = categorias[Math.floor(Math.random() * categorias.length)];
                const esRiesgo = Math.random() < 0.22;

                let monto = 0;
                let nivel = 'bajo';
                let puntaje = 0;

                if (esRiesgo) {
                    if (Math.random() < 0.45) {
                        nivel = 'critico';
                        puntaje = Math.floor(82 + Math.random() * 16);
                        monto = Math.round(cli.prom * (5 + Math.random() * 8));
                    } else {
                        nivel = 'alto';
                        puntaje = Math.floor(62 + Math.random() * 16);
                        monto = Math.round(cli.prom * (2.8 + Math.random() * 3));
                    }
                } else {
                    if (Math.random() < 0.35) {
                        nivel = 'medio';
                        puntaje = Math.floor(32 + Math.random() * 24);
                        monto = Math.round(cli.prom * (1.6 + Math.random() * 1.5));
                    } else {
                        nivel = 'bajo';
                        puntaje = Math.floor(5 + Math.random() * 22);
                        monto = Math.round(cli.prom * (0.4 + Math.random() * 0.9));
                    }
                }

                seedTransacciones.push({
                    id: `TR-${String(siguienteId++).padStart(5, '0')}`,
                    fecha: fecha,
                    tr: {
                        cliente: cli.id,
                        clienteNombre: cli.nombre,
                        monto: monto,
                        categoria: cat,
                        hora: fecha.getHours(),
                        pais: esRiesgo ? 'regional' : 'local',
                        dispositivo: esRiesgo ? 'nuevo' : 'reconocido',
                        operacionesUltimaHora: esRiesgo ? 5 : 1,
                        beneficiario: esRiesgo ? 'nuevo' : 'recurrente',
                        ip: esRiesgo ? 'nueva' : 'conocida',
                        beneficiarioNombre: destinatarios[Math.floor(Math.random() * destinatarios.length)],
                        bancoDestino: bancos[Math.floor(Math.random() * bancos.length)],
                        cuentaDestino: '191-' + Math.floor(1000000 + Math.random() * 9000000) + '-0-11',
                        concepto: 'Operación ' + cat
                    },
                    veredicto: {
                        puntaje: puntaje,
                        nivel: nivel,
                        bloqueada: nivel === 'critico',
                        enRevision: nivel === 'alto',
                        factores: []
                    }
                });
            }
        }

        // Ordenar del más reciente al más antiguo
        seedTransacciones.sort((a, b) => new Date(b.fecha) - new Date(a.fecha));

        return {
            transacciones: seedTransacciones,
            siguienteId: siguienteId,
            filtroActivo: 'todas',
            intervaloAutoId: null
        };
    }

    /* ---------------------------------------------------------
       Cálculo y Actualización de Métricas Clave (KPIs)
       --------------------------------------------------------- */
    function actualizarKPIs(transacciones) {
        const total = transacciones.length;
        const volumenTotal = transacciones.reduce((sum, t) => sum + (Number(t.tr.monto) || 0), 0);
        
        const bajo = transacciones.filter(t => t.veredicto.nivel === 'bajo').length;
        const medio = transacciones.filter(t => t.veredicto.nivel === 'medio').length;
        const alto = transacciones.filter(t => t.veredicto.nivel === 'alto').length;
        const critico = transacciones.filter(t => t.veredicto.nivel === 'critico').length;
        
        const tasaBajo = total ? ((bajo / total) * 100).toFixed(1) : '0.0';
        const tasaMedio = total ? ((medio / total) * 100).toFixed(1) : '0.0';
        const tasaAlto = total ? ((alto / total) * 100).toFixed(1) : '0.0';
        const tasaCritico = total ? ((critico / total) * 100).toFixed(1) : '0.0';
        
        const scorePromedio = total ? (transacciones.reduce((sum, t) => sum + (t.veredicto.puntaje || 0), 0) / total).toFixed(1) : '0.0';

        // Elementos DOM
        const elVolumen = document.getElementById('kpiVolumenTotal');
        const elTotal = document.getElementById('kpiTotalTrx');
        
        const elAprobadas = document.getElementById('kpiAprobadas');
        const elTasaAprobadas = document.getElementById('kpiTasaAprobadas');
        
        const elMonitoreo = document.getElementById('kpiMonitoreo');
        const elTasaMonitoreo = document.getElementById('kpiTasaMonitoreo');
        
        const elRevision = document.getElementById('kpiRevision');
        const elTasaRevision = document.getElementById('kpiTasaRevision');
        
        const elBloqueadas = document.getElementById('kpiBloqueadas');
        const elTasaBloqueadas = document.getElementById('kpiTasaBloqueadas');
        
        const elScore = document.getElementById('kpiScorePromedio');

        if (elVolumen) elVolumen.textContent = `S/ ${volumenTotal.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
        if (elTotal) elTotal.textContent = total.toLocaleString('es-PE');
        
        if (elAprobadas) elAprobadas.textContent = bajo.toLocaleString('es-PE');
        if (elTasaAprobadas) elTasaAprobadas.textContent = `(${tasaBajo}%)`;
        
        if (elMonitoreo) elMonitoreo.textContent = medio.toLocaleString('es-PE');
        if (elTasaMonitoreo) elTasaMonitoreo.textContent = `(${tasaMedio}%)`;
        
        if (elRevision) elRevision.textContent = alto.toLocaleString('es-PE');
        if (elTasaRevision) elTasaRevision.textContent = `(${tasaAlto}%)`;
        
        if (elBloqueadas) elBloqueadas.textContent = critico.toLocaleString('es-PE');
        if (elTasaBloqueadas) elTasaBloqueadas.textContent = `(${tasaCritico}%)`;
        
        if (elScore) elScore.textContent = scorePromedio;
    }

    /* ---------------------------------------------------------
       Gráfico 1: Histórico de Transferencias en el Tiempo
       --------------------------------------------------------- */
    function renderizarGraficoHistorico(transacciones, dias = 30) {
        const ctx = document.getElementById('chartHistorico');
        if (!ctx) return;

        const hoy = new Date();
        const diasLabels = [];
        const datosPorDia = {};
        
        // Helper to format local date safely to YYYY-MM-DD
        const getLocalKey = (d) => {
            const yyyy = d.getFullYear();
            const mm = String(d.getMonth() + 1).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        };

        // Construir rango de días
        for (let i = dias - 1; i >= 0; i--) {
            const d = new Date(hoy);
            d.setDate(hoy.getDate() - i);
            const key = getLocalKey(d);
            datosPorDia[key] = { volumen: 0, cantidad: 0 };
            
            const labelStr = d.toLocaleDateString('es-PE', dias <= 15 ? { day: '2-digit', month: 'short' } : { day: 'numeric', month: 'short' });
            diasLabels.push({ key, label: labelStr });
        }

        // Acumular transacciones
        transacciones.forEach(t => {
            const fecha = new Date(t.fecha);
            const key = getLocalKey(fecha);
            if (datosPorDia[key]) {
                datosPorDia[key].volumen += Number(t.tr.monto) || 0;
                datosPorDia[key].cantidad += 1;
            }
        });

        const labels = diasLabels.map(d => d.label);
        const volumenData = diasLabels.map(d => Math.round(datosPorDia[d.key].volumen));

        if (chartHistorico) {
            chartHistorico.data.labels = labels;
            chartHistorico.data.datasets[0].data = volumenData;
            chartHistorico.update();
            return;
        }

        chartHistorico = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Volumen Transferido (S/)',
                    data: volumenData,
                    borderColor: COLOR_ROJO,
                    backgroundColor: 'rgba(200, 16, 46, 0.08)',
                    borderWidth: 2.5,
                    pointRadius: dias <= 15 ? 3 : 2,
                    pointHoverRadius: 5,
                    pointBackgroundColor: COLOR_ROJO,
                    tension: 0.35,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: COLOR_NEGRO,
                        titleColor: '#ffffff',
                        bodyColor: '#ffffff',
                        padding: 10,
                        cornerRadius: 6,
                        callbacks: {
                            label: (item) => `Volumen: S/ ${Number(item.raw).toLocaleString('es-PE')}`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { color: COLOR_GRIS, maxTicksLimit: 10 }
                    },
                    y: {
                        grid: { color: COLOR_BORDE },
                        ticks: {
                            color: COLOR_GRIS,
                            callback: (val) => `S/ ${val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val}`
                        }
                    }
                }
            }
        });
    }

    /* ---------------------------------------------------------
       Gráfico 2: Desglose por Nivel de Riesgo (Doughnut)
       --------------------------------------------------------- */
    function renderizarGraficoRiesgo(transacciones) {
        const ctx = document.getElementById('chartRiesgo');
        if (!ctx) return;

        const bajo = transacciones.filter(t => t.veredicto.nivel === 'bajo').length;
        const medio = transacciones.filter(t => t.veredicto.nivel === 'medio').length;
        const alto = transacciones.filter(t => t.veredicto.nivel === 'alto').length;
        const critico = transacciones.filter(t => t.veredicto.nivel === 'critico').length;

        // Actualizar valores bajo el gráfico
        if (document.getElementById('legendBajoVal')) document.getElementById('legendBajoVal').textContent = bajo;
        if (document.getElementById('legendMedioVal')) document.getElementById('legendMedioVal').textContent = medio;
        if (document.getElementById('legendAltoVal')) document.getElementById('legendAltoVal').textContent = alto;
        if (document.getElementById('legendCriticoVal')) document.getElementById('legendCriticoVal').textContent = critico;

        const dataValores = [bajo, medio, alto, critico];

        if (chartRiesgo) {
            chartRiesgo.data.datasets[0].data = dataValores;
            chartRiesgo.update();
            return;
        }

        chartRiesgo = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Bajo (Aprobada)', 'Medio (Monitoreo)', 'Alto (Revisión)', 'Crítico (Rechazada)'],
                datasets: [{
                    data: dataValores,
                    backgroundColor: [
                        COLOR_RIESGO_BAJO,
                        COLOR_RIESGO_MEDIO,
                        COLOR_RIESGO_ALTO,
                        COLOR_RIESGO_CRITICO
                    ],
                    borderWidth: 2,
                    borderColor: '#ffffff',
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '68%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            padding: 12,
                            font: { size: 11, family: 'Inter' },
                            color: COLOR_NEGRO
                        }
                    },
                    tooltip: {
                        backgroundColor: COLOR_NEGRO,
                        padding: 10,
                        cornerRadius: 6,
                        callbacks: {
                            label: (item) => ` ${item.label}: ${item.raw} ops`
                        }
                    }
                }
            }
        });
    }

    /* ---------------------------------------------------------
       Gráfico 3: Distribución por Tipo / Rubro de Transacción (Barras)
       --------------------------------------------------------- */
    function renderizarGraficoCategorias(transacciones) {
        const ctx = document.getElementById('chartCategorias');
        if (!ctx) return;

        const categoriasNombres = {
            retail: 'Retail / Compras',
            servicios: 'Servicios',
            restaurante: 'Restaurantes',
            electronica: 'Electrónica',
            casino: 'Casino / Apuestas',
            cripto: 'Criptomonedas'
        };

        const conteos = { retail: 0, servicios: 0, restaurante: 0, electronica: 0, casino: 0, cripto: 0 };
        
        transacciones.forEach(t => {
            const cat = t.tr.categoria;
            if (conteos.hasOwnProperty(cat)) {
                conteos[cat]++;
            } else {
                conteos['retail']++;
            }
        });

        const labels = Object.keys(conteos).map(k => categoriasNombres[k]);
        const dataValores = Object.values(conteos);

        if (chartCategorias) {
            chartCategorias.data.labels = labels;
            chartCategorias.data.datasets[0].data = dataValores;
            chartCategorias.update();
            return;
        }

        chartCategorias = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Operaciones',
                    data: dataValores,
                    backgroundColor: 'rgba(200, 16, 46, 0.85)',
                    hoverBackgroundColor: COLOR_ROJO,
                    borderRadius: 4,
                    maxBarThickness: 34
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: COLOR_NEGRO,
                        padding: 10,
                        cornerRadius: 6,
                        callbacks: {
                            label: (item) => ` ${item.raw} operaciones`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { color: COLOR_GRIS, font: { size: 10 } }
                    },
                    y: {
                        grid: { color: COLOR_BORDE },
                        ticks: { color: COLOR_GRIS, stepSize: 1 }
                    }
                }
            }
        });
    }

    /* ---------------------------------------------------------
       Gráfico 4: Incidencias de Riesgo por Hora (Líneas)
       --------------------------------------------------------- */
    function renderizarGraficoHorasRiesgo(transacciones) {
        const ctx = document.getElementById('chartHorasRiesgo');
        if (!ctx) return;

        // Inicializar 24 horas a 0
        const horas = Array.from({ length: 24 }, (_, i) => 0);
        
        // Filtrar transacciones de riesgo alto o crítico
        const incidentes = transacciones.filter(t => t.veredicto.nivel === 'alto' || t.veredicto.nivel === 'critico');
        
        incidentes.forEach(t => {
            const h = Number(t.tr.hora) || 0;
            if (h >= 0 && h < 24) horas[h]++;
        });

        const labels = Array.from({ length: 24 }, (_, i) => `${i}:00`);

        if (chartHorasRiesgo) {
            chartHorasRiesgo.data.datasets[0].data = horas;
            chartHorasRiesgo.update();
            return;
        }

        chartHorasRiesgo = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Operaciones de Riesgo',
                    data: horas,
                    borderColor: COLOR_RIESGO_CRITICO,
                    backgroundColor: 'rgba(156, 12, 35, 0.1)',
                    borderWidth: 2,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: COLOR_NEGRO,
                        padding: 10,
                        cornerRadius: 6,
                        callbacks: {
                            label: (item) => ` ${item.raw} operaciones`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { color: COLOR_GRIS, maxTicksLimit: 12 }
                    },
                    y: {
                        grid: { color: COLOR_BORDE },
                        ticks: { color: COLOR_GRIS, stepSize: 1 }
                    }
                }
            }
        });
    }

    /* ---------------------------------------------------------
       Gráfico 5: Principales Bancos Destino (Doughnut/Bar)
       --------------------------------------------------------- */
    function renderizarGraficoBancos(transacciones) {
        const ctx = document.getElementById('chartBancos');
        if (!ctx) return;

        const conteos = {};
        
        transacciones.forEach(t => {
            const banco = t.tr.bancoDestino || 'Otro';
            conteos[banco] = (conteos[banco] || 0) + 1;
        });

        // Ordenar bancos por volumen de transacciones (mayor a menor)
        const bancosOrdenados = Object.entries(conteos)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5); // Top 5 bancos

        const labels = bancosOrdenados.map(b => b[0]);
        const dataValores = bancosOrdenados.map(b => b[1]);

        if (chartBancos) {
            chartBancos.data.labels = labels;
            chartBancos.data.datasets[0].data = dataValores;
            chartBancos.update();
            return;
        }

        chartBancos = new Chart(ctx, {
            type: 'bar', // Barra horizontal
            data: {
                labels: labels,
                datasets: [{
                    label: 'Operaciones',
                    data: dataValores,
                    backgroundColor: [
                        '#111111',
                        '#333333',
                        '#555555',
                        '#777777',
                        '#999999'
                    ],
                    borderRadius: 4,
                    maxBarThickness: 25
                }]
            },
            options: {
                indexAxis: 'y', // Convertir a barras horizontales
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: COLOR_NEGRO,
                        padding: 10,
                        cornerRadius: 6,
                        callbacks: {
                            label: (item) => ` ${item.raw} operaciones`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { color: COLOR_BORDE },
                        ticks: { color: COLOR_GRIS, stepSize: 1 }
                    },
                    y: {
                        grid: { display: false },
                        ticks: { color: COLOR_NEGRO, font: { weight: '500' } }
                    }
                }
            }
        });
    }

    /* ---------------------------------------------------------
       Tabla de Incidentes y Operaciones Críticas Recientes
       --------------------------------------------------------- */
    function renderizarTablaIncidentes(transacciones) {
        const tbody = document.getElementById('tablaIncidentesRecientes');
        if (!tbody) return;

        // Filtrar transacciones en riesgo alto o crítico, o con mayor score
        const incidentes = transacciones
            .filter(t => t.veredicto.nivel === 'alto' || t.veredicto.nivel === 'critico')
            .slice(0, 6);

        tbody.innerHTML = '';

        if (incidentes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-secondary py-4">No se registran incidentes sospechosos ni bloqueos.</td></tr>`;
            return;
        }

        incidentes.forEach(item => {
            const nivel = item.veredicto.nivel;
            const esCritico = nivel === 'critico';
            const badge = esCritico
                ? `<span class="badge" style="background-color: rgba(200, 16, 46, 0.12); color: var(--riesgo-critico); border: 1px solid var(--riesgo-critico);"><i class="bi bi-x-octagon-fill me-1"></i> Bloqueada</span>`
                : `<span class="badge" style="background-color: rgba(194, 65, 12, 0.12); color: var(--riesgo-alto); border: 1px solid var(--riesgo-alto);"><i class="bi bi-hourglass-split me-1"></i> Revisión Manual</span>`;

            const colorScore = esCritico ? 'var(--riesgo-critico)' : 'var(--riesgo-alto)';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="dato-mono fw-bold small">${item.id}</td>
                <td>
                    <div class="small fw-semibold" style="color: var(--texto-principal);">${item.tr.clienteNombre}</div>
                    <div class="text-secondary" style="font-size: 0.75rem;">Destino: ${item.tr.beneficiarioNombre || 'Externo'}</div>
                </td>
                <td class="dato-mono fw-semibold small">S/ ${item.tr.monto.toFixed(2)}</td>
                <td class="dato-mono fw-bold small" style="color: ${colorScore};">${item.veredicto.puntaje}</td>
                <td>${badge}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    /* ---------------------------------------------------------
       Carga y Actualización Integral del Dashboard
       --------------------------------------------------------- */
    function actualizarDashboard() {
        const estado = obtenerEstadoTransacciones();
        const transacciones = estado.transacciones;

        actualizarKPIs(transacciones);
        renderizarGraficoHistorico(transacciones, rangoDiasActivo);
        renderizarGraficoRiesgo(transacciones);
        renderizarGraficoCategorias(transacciones);
        renderizarGraficoHorasRiesgo(transacciones);
        renderizarGraficoBancos(transacciones);
        renderizarTablaIncidentes(transacciones);
    }

    /* ---------------------------------------------------------
       Eventos de Interfaz
       --------------------------------------------------------- */
    // Botones de rango temporal para el gráfico histórico
    document.querySelectorAll('.lambda-range-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.lambda-range-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            rangoDiasActivo = parseInt(btn.dataset.dias, 10) || 30;
            const estado = obtenerEstadoTransacciones();
            renderizarGraficoHistorico(estado.transacciones, rangoDiasActivo);
        });
    });

    // Botón de recargar métricas
    const btnRecargar = document.getElementById('btnRecargarDatos');
    if (btnRecargar) {
        btnRecargar.addEventListener('click', () => {
            actualizarDashboard();
        });
    }

    // Botón Cerrar Sesión
    const btnCerrar = document.getElementById('btnCerrarSesion');
    if (btnCerrar) {
        btnCerrar.addEventListener('click', blCerrarSesion);
    }

    /* ---------------------------------------------------------
       Inicialización
       --------------------------------------------------------- */
    actualizarDashboard();

})();

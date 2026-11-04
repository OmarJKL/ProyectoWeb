(() => {
    'use strict';

    const tabla = document.getElementById('tablaTransacciones');
    const btnFiltroList = document.querySelectorAll('.btn-filtro');
    const inputMonto = document.getElementById('montoFiltro');
    const selectMonto = document.getElementById('comparadorMonto');
    const btnAplicarMonto = document.getElementById('btnAplicarMonto');
    const btnLimpiarMonto = document.getElementById('btnLimpiarMonto');
    const mesFiltro = document.getElementById('mesFiltro');
    const anioFiltro = document.getElementById('anioFiltro');
    const btnAplicarFecha = document.getElementById('btnAplicarFecha');
    const btnLimpiarFecha = document.getElementById('btnLimpiarFecha');

    const rol = document.body.dataset.rol || '';
    const esAdmin = rol === 'ADMIN';

    let transacciones = [];
    let filtroActivo = 'todas';
    let filtroMonto = null;
    let filtroFecha = null;

    function getNivelLabel(nivel) {
        return nivel === 'bajo' ? 'Bajo' : nivel === 'medio' ? 'Medio' : nivel === 'alto' ? 'Alto' : 'Crítico';
    }

    function escapeHtml(texto) {
        const span = document.createElement('span');
        span.textContent = texto == null ? '' : String(texto);
        return span.innerHTML;
    }

    function fechaDe(tx) {
        return tx.fechaHora ? new Date(tx.fechaHora) : null;
    }

    async function cargar() {
        const [resTx, resKpi] = await Promise.all([
            LSApi.fetch('/api/v1/evaluation?activas=false'),
            LSApi.fetch('/api/v1/metric')
        ]);
        transacciones = await resTx.json();
        pintarKpis(await resKpi.json());
        renderizarLista();
    }

    function pintarKpis(kpis) {
        document.getElementById('kpiTotal').textContent = kpis.total;
        document.getElementById('kpiPuntaje').textContent = Number(kpis.puntajePromedio).toFixed(1);
        document.getElementById('kpiAprobadas').textContent = kpis.aprobadas;
        document.getElementById('kpiRevision').textContent = kpis.monitoreadas + kpis.enRevision;
        document.getElementById('kpiCritico').textContent = kpis.bloqueadas;
    }

    function coincidirVista(tx) {
        if (filtroActivo === 'desactivadas') return !tx.activa;
        return tx.activa && (filtroActivo === 'todas' || filtroActivo === tx.nivel);
    }

    function cumplirMonto(tx) {
        if (filtroMonto === null) return true;
        const valor = Number(filtroMonto.valor);
        if (Number.isNaN(valor)) return true;
        return filtroMonto.comparador === 'mayor' ? tx.monto > valor : tx.monto < valor;
    }

    function cumplirFecha(tx) {
        if (!filtroFecha) return true;
        const fecha = fechaDe(tx);
        if (!fecha) return true;
        if (filtroFecha.mes && (fecha.getMonth() + 1) !== Number(filtroFecha.mes)) return false;
        if (filtroFecha.anio && fecha.getFullYear() !== Number(filtroFecha.anio)) return false;
        return true;
    }

    function renderizarLista() {
        if (!tabla) return;

        const filtradas = transacciones.filter(tx =>
            coincidirVista(tx) && cumplirMonto(tx) && cumplirFecha(tx));

        tabla.innerHTML = '';

        filtradas.forEach(tx => {
            const tr = document.createElement('tr');
            if (!tx.activa) {
                tr.style.opacity = '0.5';
                tr.style.filter = 'grayscale(0.6)';
            }

            const estadoHtml = tx.activa
                ? `<span style="color:${tx.colorNivel}; font-size:0.8rem; font-weight:600;">
                       <i class="bi ${tx.iconoEstado}"></i> ${escapeHtml(tx.estado)}
                   </span>`
                : `<span class="text-secondary" style="font-size:0.8rem; font-weight:600;">
                       <i class="bi bi-eye-slash"></i> Desactivada
                   </span>`;

            const accionHtml = esAdmin
                ? (tx.activa
                    ? `<button type="button" class="btn btn-sm btn-secundario btn-accion-desactivar">
                           <i class="bi bi-eye-slash"></i> Desactivar
                       </button>`
                    : `<button type="button" class="btn btn-sm btn-success btn-accion-reactivar">
                           <i class="bi bi-arrow-counterclockwise"></i> Reactivar
                       </button>`)
                : '';

            tr.innerHTML = `
                <td class="dato-mono fw-bold">${escapeHtml(tx.id)}</td>
                <td>${escapeHtml(tx.clienteNombre)}</td>
                <td class="dato-mono">S/ ${Number(tx.monto).toFixed(2)}</td>
                <td>${escapeHtml(tx.categoria)}</td>
                <td>${escapeHtml(tx.pais)}</td>
                <td class="dato-mono">${String(tx.hora).padStart(2, '0')}:00</td>
                <td class="dato-mono fw-bold" style="color:${tx.colorNivel};">${tx.puntaje}</td>
                <td>
                    <span class="etiqueta-nivel" style="background-color:${tx.colorNivel}22; color:${tx.colorNivel}; border: 1px solid ${tx.colorNivel};">
                        ${getNivelLabel(tx.nivel)}
                    </span>
                </td>
                <td>${estadoHtml}</td>
                <td class="dato-mono">${escapeHtml(tx.fecha)}</td>
                <td>
                    <button type="button" class="btn btn-sm btn-danger btn-detalle-transaccion">
                        <i class="bi bi-file-earmark-text"></i> Ver detalles
                    </button>
                </td>
                <td>
                    <button type="button" class="btn btn-sm btn-outline-danger btn-ver-historial">
                        <i class="bi bi-clock-history"></i> Ver historial
                    </button>
                </td>
                ${esAdmin ? `<td>${accionHtml}</td>` : ''}
            `;

            tr.querySelector('.btn-detalle-transaccion').addEventListener('click', () => abrirDetalle(tx));
            tr.querySelector('.btn-ver-historial').addEventListener('click', () => abrirHistorial(tx));

            const btnDesactivar = tr.querySelector('.btn-accion-desactivar');
            if (btnDesactivar) {
                btnDesactivar.addEventListener('click', () => confirmarDesactivar(tx));
            }
            const btnReactivar = tr.querySelector('.btn-accion-reactivar');
            if (btnReactivar) {
                btnReactivar.addEventListener('click', () => confirmarReactivar(tx));
            }

            tabla.appendChild(tr);
        });
    }

    const confirmarModal = () => document.getElementById('confirmarAccionModal');

    function pedirConfirmacion(titulo, mensaje, accion) {
        const modal = confirmarModal();
        document.getElementById('confirmarTitulo').textContent = titulo;
        document.getElementById('confirmarMensaje').textContent = mensaje;
        const btnAceptar = document.getElementById('confirmarAceptar');

        // Se clona para quitar oyentes previos; hay que habilitarlo de nuevo,
        // porque cloneNode copia el estado disabled del botón anterior.
        const nuevo = btnAceptar.cloneNode(true);
        btnAceptar.replaceWith(nuevo);
        nuevo.disabled = false;
        nuevo.addEventListener('click', async () => {
            nuevo.disabled = true;
            try {
                await accion();
                bootstrap.Modal.getOrCreateInstance(modal).hide();
                await cargar();
            } catch (e) {
                nuevo.disabled = false;
            }
        });

        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function confirmarDesactivar(tx) {
        pedirConfirmacion(
            'Desactivar operación',
            `La operación ${tx.id} de ${tx.clienteNombre} se marcará como inactiva (es_activa = false) y su transacción pasará a REVERSADA.`,
            async () => {
                const res = await LSApi.fetch(`/api/v1/evaluation/${tx.idInterno}`, { method: 'PATCH' });
                if (!res.ok && res.status !== 204) {
                    LSApi.mostrarAviso('No se pudo desactivar la operación.');
                    throw new Error('desactivar');
                }
                LSApi.mostrarAviso(`Operación ${tx.id} desactivada.`);
            });
    }

    function confirmarReactivar(tx) {
        pedirConfirmacion(
            'Reactivar operación',
            `La operación ${tx.id} de ${tx.clienteNombre} volverá a estar activa y su transacción recuperará su estado según el nivel de riesgo.`,
            async () => {
                const res = await LSApi.fetch(`/api/v1/evaluation/${tx.idInterno}/activar`, { method: 'PATCH' });
                if (!res.ok && res.status !== 204) {
                    LSApi.mostrarAviso('No se pudo reactivar la operación.');
                    throw new Error('reactivar');
                }
                LSApi.mostrarAviso(`Operación ${tx.id} reactivada.`);
            });
    }

    function abrirDetalle(tx) {
        const modal = document.getElementById('detalleTransaccionModal');
        if (!modal) return;

        document.getElementById('detalleCliente').textContent = tx.clienteNombre;
        document.getElementById('detalleFecha').textContent = tx.fecha;
        document.getElementById('detalleMonto').textContent = 'S/ ' + Number(tx.monto).toFixed(2);
        document.getElementById('detallePuntaje').textContent = tx.puntaje;
        document.getElementById('detalleNivel').textContent = getNivelLabel(tx.nivel);

        const detalleEstado = document.getElementById('detalleEstado');
        if (!tx.activa) {
            detalleEstado.textContent = 'Desactivada: la evaluación fue dada de baja (es_activa = false).';
        } else if (tx.nivel === 'bajo') {
            detalleEstado.textContent = 'Aprobada: no presenta riesgo significativo.';
        } else if (tx.nivel === 'medio') {
            detalleEstado.textContent = 'Monitoreo: requiere revisión por comportamiento atípico.';
        } else if (tx.nivel === 'alto') {
            detalleEstado.textContent = 'Retener / Alertar: la operación excede el rango de comportamiento esperado.';
        } else {
            detalleEstado.textContent = 'Bloqueada: la operación fue detenida por riesgo crítico.';
        }

        const lista = document.getElementById('detalleListaFactores');
        lista.innerHTML = '';
        (tx.factores || []).forEach(factor => {
            const li = document.createElement('li');
            li.innerHTML = `<strong>${escapeHtml(factor.etiqueta)}</strong>: ${factor.puntaje} / ${factor.max} — ${escapeHtml(factor.motivo)}`;
            lista.appendChild(li);
        });

        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function abrirHistorial(txBase) {
        const modal = document.getElementById('historialClienteModal');
        if (!modal) return;

        const clienteTx = transacciones.filter(t => t.clienteCodigo === txBase.clienteCodigo);
        const total = clienteTx.reduce((sum, t) => sum + t.monto, 0);

        document.getElementById('historialClienteTitulo').textContent = 'Historial de ' + txBase.clienteNombre;
        document.getElementById('historialClienteSubtitulo').textContent =
            clienteTx.length + ' evaluaciones registradas';
        document.getElementById('clienteSaldo').textContent = 'S/ ' + total.toFixed(2);
        document.getElementById('clienteNombreTarjeta').textContent = txBase.clienteNombre;

        const tbodyHistorial = document.getElementById('tablaHistorialCliente');
        tbodyHistorial.innerHTML = '';

        clienteTx.forEach(tx => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${escapeHtml(tx.id)}</td>
                <td>${escapeHtml(tx.fecha)}</td>
                <td>S/ ${Number(tx.monto).toFixed(2)}</td>
                <td>${escapeHtml(tx.categoria)}</td>
                <td>${getNivelLabel(tx.nivel)}</td>
                <td>${tx.activa ? escapeHtml(tx.estado) : 'Desactivada'}</td>
            `;
            tbodyHistorial.appendChild(row);
        });

        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    btnFiltroList.forEach(btn => {
        btn.addEventListener('click', () => {
            btnFiltroList.forEach(b => {
                b.classList.remove('btn-principal');
                b.classList.add('btn-secundario');
            });
            btn.classList.remove('btn-secundario');
            btn.classList.add('btn-principal');
            filtroActivo = btn.dataset.filtro;
            renderizarLista();
        });
    });

    btnAplicarMonto.addEventListener('click', () => {
        const valor = inputMonto.value;
        filtroMonto = valor === '' ? null : { valor: Number(valor), comparador: selectMonto.value };
        renderizarLista();
    });

    btnLimpiarMonto.addEventListener('click', () => {
        inputMonto.value = '';
        filtroMonto = null;
        renderizarLista();
    });

    btnAplicarFecha.addEventListener('click', () => {
        filtroFecha = { mes: mesFiltro.value, anio: anioFiltro.value };
        renderizarLista();
    });

    btnLimpiarFecha.addEventListener('click', () => {
        mesFiltro.value = '';
        anioFiltro.value = '';
        filtroFecha = null;
        renderizarLista();
    });

    cargar().catch(() => {
        LSApi.mostrarAviso('No se pudo cargar el historial de operaciones.');
    });
})();

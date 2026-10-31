(() => {
    'use strict';
    let filtroActivo = 'todas';
    let filtroMonto = null;
    const cuerpoTabla = document.getElementById('tablaTransacciones');

    function coincideFiltro(nivel) {
        return filtroActivo === 'todas' || filtroActivo === nivel;
    }

    function montoComparado(monto) {
        if (filtroMonto === null) return true;
        const valor = Number(filtroMonto.valor);
        if (Number.isNaN(valor)) return true;

        const actual = Number(monto);
        switch (filtroMonto.comparador) {
            case 'mayor': return actual > valor;
            case 'menor': return actual < valor;
            case 'igual': return Math.abs(actual - valor) < 0.0001;
            default: return true;
        }
    }

    function reaplicarFiltros() {
        if (!cuerpoTabla) return;
        cuerpoTabla.querySelectorAll('tr[data-nivel]').forEach(tr => {
            const nivel = tr.dataset.nivel;
            const monto = tr.dataset.monto;
            const cumpleNivel = coincideFiltro(nivel);
            const cumpleMonto = montoComparado(monto);
            tr.style.display = (cumpleNivel && cumpleMonto) ? '' : 'none';
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
            filtroActivo = btn.dataset.filtro;
            reaplicarFiltros();
        });
    });

    const inputMonto = document.getElementById('montoFiltro');
    const selectMonto = document.getElementById('comparadorMonto');
    const btnAplicarMonto = document.getElementById('btnAplicarMonto');
    const btnLimpiarMonto = document.getElementById('btnLimpiarMonto');

    function establecerFiltroMonto(valor, comparador) {
        if (valor === null || valor === undefined || valor === '') {
            filtroMonto = null;
            reaplicarFiltros();
            return;
        }
        const numero = Number(valor);
        if (Number.isNaN(numero)) {
            return;
        }
        filtroMonto = {
            valor: numero,
            comparador: comparador || 'mayor'
        };
        reaplicarFiltros();
    }

    function leerFiltroMontoDesdeInput() {
        const valor = inputMonto ? inputMonto.value : '';
        const comparador = selectMonto ? selectMonto.value : 'mayor';
        establecerFiltroMonto(valor, comparador);
    }

    if (btnAplicarMonto) {
        btnAplicarMonto.addEventListener('click', leerFiltroMontoDesdeInput);
    }

    if (btnLimpiarMonto) {
        btnLimpiarMonto.addEventListener('click', () => {
            if (inputMonto) inputMonto.value = '';
            filtroMonto = null;
            reaplicarFiltros();
        });
    }

    document.querySelectorAll('.btn-filtro-monto').forEach(btn => {
        btn.addEventListener('click', () => {
            const tipo = btn.dataset.tipo || 'mayor';
            const valor = btn.dataset.valor || '';
            if (inputMonto) inputMonto.value = valor;
            if (selectMonto) selectMonto.value = tipo;
            establecerFiltroMonto(valor, tipo);
        });
    });

    // Soft delete: se marca la evaluación como inactiva mediante PATCH.
    document.querySelectorAll('.btn-soft-delete').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-id');
            if (!id) return;
            if (!confirm('¿Desactivar esta evaluación? La fila se conservará en la base de datos como inactiva.')) {
                return;
            }
            try {
                const res = await LSApi.fetch('/api/v1/evaluation/' + encodeURIComponent(id), {
                    method: 'PATCH'
                });
                if (res.ok) {
                    window.location.reload();
                }
            } catch (e) {
                // LSApi ya mostró el aviso de permisos o redirigió por sesión expirada.
            }
        });
    });

    async function sincronizarKpisYTabla() {
        try {
            const resKpis = await LSApi.fetch('/api/v1/metric');
            if (resKpis.ok) {
                const k = await resKpis.json();
                const elTotal = document.getElementById('kpiTotal');
                const elAprobadas = document.getElementById('kpiAprobadas');
                const elRevision = document.getElementById('kpiRevision');
                const elCritico = document.getElementById('kpiCritico');
                const elPuntaje = document.getElementById('kpiPuntaje');

                if (elTotal) elTotal.textContent = k.total;
                if (elAprobadas) elAprobadas.textContent = k.aprobadas;
                if (elRevision) elRevision.textContent = (k.monitoreadas + k.enRevision);
                if (elCritico) elCritico.textContent = k.bloqueadas;
                if (elPuntaje) elPuntaje.textContent = k.puntajePromedio.toFixed(1);
            }
        } catch (e) {
            // Silencioso en caso de error de red
        }
    }

    // Sincronizar periódicamente cada 4 segundos
    setInterval(sincronizarKpisYTabla, 4000);
})();

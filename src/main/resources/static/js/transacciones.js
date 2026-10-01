(() => {
    'use strict';
    let filtroActivo = 'todas';
    const cuerpoTabla = document.getElementById('tablaTransacciones');

    function coincideFiltro(nivel) {
        return filtroActivo === 'todas' || filtroActivo === nivel;
    }

    function reaplicarFiltro() {
        if (!cuerpoTabla) return;
        cuerpoTabla.querySelectorAll('tr[data-nivel]').forEach(tr => {
            tr.style.display = coincideFiltro(tr.dataset.nivel) ? '' : 'none';
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
            reaplicarFiltro();
        });
    });

    const btnReset = document.getElementById('btnReset');
    if (btnReset) {
        btnReset.addEventListener('click', async () => {
            if (confirm('¿Deseas reiniciar el historial de transacciones evaluadas?')) {
                await fetch('/api/v1/evaluation', { method: 'DELETE' });
                window.location.reload();
            }
        });
    }

    async function sincronizarKpisYTabla() {
        try {
            const resKpis = await fetch('/api/v1/evaluation/kpis');
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

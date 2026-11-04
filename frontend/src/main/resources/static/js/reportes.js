(() => {
    'use strict';

    const form = document.getElementById('formFiltrosReporte');
    if (!form) return;

    const botones = {
        csv: document.getElementById('btnExportCsv'),
        xlsx: document.getElementById('btnExportExcel'),
        pdf: document.getElementById('btnExportPdf')
    };

    function construirUrl(formato) {
        const params = new URLSearchParams();
        params.set('formato', formato);
        const desde = document.getElementById('filtroDesde').value;
        const hasta = document.getElementById('filtroHasta').value;
        const nivel = document.getElementById('filtroNivel').value;
        const cliente = document.getElementById('filtroCliente').value.trim();
        if (desde) params.set('desde', desde);
        if (hasta) params.set('hasta', hasta);
        if (nivel) params.set('nivel', nivel);
        if (cliente) params.set('cliente', cliente);
        return '/api/v1/report/export?' + params.toString();
    }

    function sincronizar() {
        Object.entries(botones).forEach(([formato, boton]) => {
            if (boton) boton.setAttribute('href', construirUrl(formato));
        });
    }

    form.addEventListener('input', sincronizar);
    sincronizar();
})();

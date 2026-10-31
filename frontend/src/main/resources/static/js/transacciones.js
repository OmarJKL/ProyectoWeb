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

    let filtroActivo = 'todas';
    let filtroMonto = null;
    let filtroFecha = null;

    const nombres = [
        'María Fernández Rojas',
        'Jorge Salas Mendoza',
        'Ana Torres Vega',
        'Carlos Quispe Luján',
        'Lucía Paredes Silva',
        'Renato Vargas Nole',
        'Sofía Díaz García',
        'Miguel Rojas Torres',
        'Patricia Cárdenas Huamán',
        'Daniel Flores Ayala'
    ];

    const categorias = ['electronica', 'restaurante', 'servicios', 'retail', 'electronica'];
    const ubicaciones = ['local', 'local', 'local', 'local', 'alto_riesgo'];

    function getNivelLabel(nivel) {
        return nivel === 'bajo' ? 'Bajo' : nivel === 'medio' ? 'Medio' : nivel === 'alto' ? 'Alto' : 'Crítico';
    }

    function getColorByNivel(nivel) {
        if (nivel === 'bajo') return 'var(--riesgo-bajo)';
        if (nivel === 'medio') return 'var(--riesgo-medio)';
        if (nivel === 'alto') return 'var(--riesgo-alto)';
        return 'var(--riesgo-critico)';
    }

    function getIconByNivel(nivel) {
        if (nivel === 'bajo') return 'bi-check-circle-fill';
        if (nivel === 'medio') return 'bi-eye-fill';
        if (nivel === 'alto') return 'bi-flag-fill';
        return 'bi-x-octagon-fill';
    }

    function generarDatosDemo() {
        const registros = [];
        const meses = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

        for (let i = 0; i < 100; i++) {
            const mes = meses[i % meses.length];
            const anio = 2025 + Math.floor(i / 12) % 2;
            const dia = (i % 28) + 1;
            const hora = (i * 7) % 24;
            const minuto = (i * 13) % 60;
            const monto = 80 + ((i * 37) % 900);
            const puntaje = 12 + ((i * 13) % 88);

            let nivel = 'bajo';
            if (puntaje >= 80) nivel = 'critico';
            else if (puntaje >= 60) nivel = 'alto';
            else if (puntaje >= 35) nivel = 'medio';

            const fecha = new Date(anio, mes - 1, dia, hora, minuto);
            const nombre = nombres[i % nombres.length];
            const categoria = categorias[i % categorias.length];
            const ubicacion = ubicaciones[i % ubicaciones.length];
            const estado = nivel === 'bajo' ? 'Aprobada' : nivel === 'medio' ? 'Monitoreo' : nivel === 'alto' ? 'Retener' : 'Bloqueada';

            registros.push({
                id: 'TR-' + String(i + 1).padStart(5, '0'),
                clienteNombre: nombre,
                codigoCliente: 'CLI-' + String((i % 10) + 1).padStart(3, '0'),
                monto: Number(monto.toFixed(2)),
                categoria,
                pais: ubicacion,
                hora,
                puntaje,
                nivel,
                estado,
                fechaHora: fecha,
                fechaTexto: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }),
                colorNivel: getColorByNivel(nivel),
                iconoEstado: getIconByNivel(nivel),
                factores: [
                    { etiqueta: 'Monto inusual', puntaje: 18, max: 25, motivo: 'Se detectó un monto inusual respecto al perfil del cliente.' },
                    { etiqueta: 'Ubicación', puntaje: 14, max: 20, motivo: 'La operación presenta una ubicación no habitual para este cliente.' },
                    { etiqueta: 'Horario', puntaje: 10, max: 15, motivo: 'La operación se realizó en un horario fuera del patrón habitual.' }
                ],
                saldo: 2500 + (i * 180),
                numeroTarjeta: '•••• 4821'
            });
        }

        return registros;
    }

    const transacciones = generarDatosDemo();

    function coincidirNivel(nivel) {
        return filtroActivo === 'todas' || filtroActivo === nivel;
    }

    function cumplirMonto(tx) {
        if (filtroMonto === null) return true;
        const valor = Number(filtroMonto.valor);
        if (Number.isNaN(valor)) return true;
        const actual = Number(tx.monto);
        if (filtroMonto.comparador === 'mayor') return actual > valor;
        if (filtroMonto.comparador === 'menor') return actual < valor;
        return true;
    }

    function cumplirFecha(tx) {
        if (!filtroFecha) return true;
        const fecha = tx.fechaHora;
        const mes = fecha.getMonth() + 1;
        const anio = fecha.getFullYear();

        if (filtroFecha.mes && filtroFecha.mes !== '') {
            if (Number(mes) !== Number(filtroFecha.mes)) return false;
        }

        if (filtroFecha.anio && filtroFecha.anio !== '') {
            if (Number(anio) !== Number(filtroFecha.anio)) return false;
        }

        return true;
    }

    function renderizarLista() {
        if (!tabla) return;

        const filtradas = transacciones.filter(tx => {
            return coincidirNivel(tx.nivel) && cumplirMonto(tx) && cumplirFecha(tx);
        });

        tabla.innerHTML = '';

        filtradas.forEach(tx => {
            const tr = document.createElement('tr');
            tr.dataset.nivel = tx.nivel;
            tr.dataset.monto = String(tx.monto);
            tr.dataset.fecha = tx.fechaTexto;

            tr.innerHTML = `
                <td class="dato-mono fw-bold">${tx.id}</td>
                <td>${tx.clienteNombre}</td>
                <td class="dato-mono">S/ ${Number(tx.monto).toFixed(2)}</td>
                <td>${tx.categoria}</td>
                <td>${tx.pais}</td>
                <td class="dato-mono">${String(tx.hora).padStart(2, '0')}:00</td>
                <td class="dato-mono fw-bold" style="color:${tx.colorNivel};">${tx.puntaje}</td>
                <td>
                    <span class="etiqueta-nivel" style="background-color:${tx.colorNivel}22; color:${tx.colorNivel}; border: 1px solid ${tx.colorNivel};">
                        ${getNivelLabel(tx.nivel)}
                    </span>
                </td>
                <td>
                    <span style="color:${tx.colorNivel}; font-size:0.8rem; font-weight:600;">
                        <i class="bi ${tx.iconoEstado}"></i>
                        <span>${tx.estado}</span>
                    </span>
                </td>
                <td class="dato-mono">${tx.fechaTexto}</td>
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
                <td>
                    <button type="button" class="btn btn-sm btn-secundario btn-soft-delete">
                        <i class="bi bi-eye-slash"></i> Desactivar
                    </button>
                </td>
            `;

            const detalleBtn = tr.querySelector('.btn-detalle-transaccion');
            detalleBtn.addEventListener('click', () => abrirDetalle(tx));

            const historialBtn = tr.querySelector('.btn-ver-historial');
            historialBtn.addEventListener('click', () => abrirHistorial(tx.clienteNombre));

            const desactivarBtn = tr.querySelector('.btn-soft-delete');
            desactivarBtn.addEventListener('click', () => {
                tr.style.opacity = '0.45';
                tr.style.filter = 'grayscale(0.7)';
                alert('Transacción marcada como desactivada.');
            });

            tabla.appendChild(tr);
        });
    }

    function abrirDetalle(tx) {
        const modal = document.getElementById('detalleTransaccionModal');
        if (!modal) return;

        document.getElementById('detalleCliente').textContent = tx.clienteNombre;
        document.getElementById('detalleFecha').textContent = tx.fechaTexto;
        document.getElementById('detalleMonto').textContent = 'S/ ' + Number(tx.monto).toFixed(2);
        document.getElementById('detallePuntaje').textContent = tx.puntaje;
        document.getElementById('detalleNivel').textContent = getNivelLabel(tx.nivel);

        const detalleEstado = document.getElementById('detalleEstado');
        if (tx.nivel === 'bajo') {
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

        tx.factores.forEach(factor => {
            const li = document.createElement('li');
            li.innerHTML = `<strong>${factor.etiqueta}</strong>: ${factor.puntaje} / ${factor.max} — ${factor.motivo}`;
            lista.appendChild(li);
        });

        const modalInstance = bootstrap.Modal.getOrCreateInstance(modal);
        modalInstance.show();
    }

    function abrirHistorial(cliente) {
        const modal = document.getElementById('historialClienteModal');
        if (!modal) return;

        const clienteTx = transacciones.filter(tx => tx.clienteNombre === cliente);
        const total = clienteTx.reduce((sum, tx) => sum + tx.monto, 0);

        document.getElementById('historialClienteTitulo').textContent = 'Historial de ' + cliente;
        document.getElementById('historialClienteSubtitulo').textContent = clienteTx.length + ' transacciones registradas';
        document.getElementById('clienteSaldo').textContent = 'S/ ' + total.toFixed(2);
        document.getElementById('clienteNombreTarjeta').textContent = cliente;

        const tbodyHistorial = document.getElementById('tablaHistorialCliente');
        tbodyHistorial.innerHTML = '';

        clienteTx.forEach(tx => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${tx.id}</td>
                <td>${tx.fechaTexto}</td>
                <td>S/ ${Number(tx.monto).toFixed(2)}</td>
                <td>${tx.categoria}</td>
                <td>${getNivelLabel(tx.nivel)}</td>
                <td>${tx.estado}</td>
            `;
            tbodyHistorial.appendChild(row);
        });

        const modalInstance = bootstrap.Modal.getOrCreateInstance(modal);
        modalInstance.show();
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
        filtroMonto = valor === '' ? null : {
            valor: Number(valor),
            comparador: selectMonto.value
        };
        renderizarLista();
    });

    btnLimpiarMonto.addEventListener('click', () => {
        inputMonto.value = '';
        filtroMonto = null;
        renderizarLista();
    });

    btnAplicarFecha.addEventListener('click', () => {
        filtroFecha = {
            mes: mesFiltro.value,
            anio: anioFiltro.value
        };
        renderizarLista();
    });

    btnLimpiarFecha.addEventListener('click', () => {
        mesFiltro.value = '';
        anioFiltro.value = '';
        filtroFecha = null;
        renderizarLista();
    });

    renderizarLista();
})();

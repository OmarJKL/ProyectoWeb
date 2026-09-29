(() => {
    'use strict';

    const NIVEL_META = {
        bajo:     { etiqueta: 'Bajo',     color: 'var(--riesgo-bajo)' },
        medio:    { etiqueta: 'Medio',    color: 'var(--riesgo-medio)' },
        alto:     { etiqueta: 'Alto',     color: 'var(--riesgo-alto)' },
        critico:  { etiqueta: 'Crítico',  color: 'var(--riesgo-critico)' },
    };

    const ESTADO_POR_NIVEL = {
        bajo:    { texto: 'Aprobada',        icono: 'bi-check-circle-fill', color: 'var(--riesgo-bajo)' },
        medio:   { texto: 'Monitoreo',       icono: 'bi-eye-fill',          color: 'var(--riesgo-medio)' },
        alto:    { texto: 'Revisión manual', icono: 'bi-flag-fill',         color: 'var(--riesgo-alto)' },
        critico: { texto: 'Bloqueada',       icono: 'bi-x-octagon-fill',    color: 'var(--riesgo-critico)' },
    };

    const $ = (sel) => document.querySelector(sel);
    const form            = $('#formTransaccion');
    const resultadoVacio  = $('#resultadoVacio');
    const resultadoLleno  = $('#resultadoLleno');
    const anilloPuntaje   = $('#anilloPuntaje');
    const numeroPuntaje   = $('#numeroPuntaje');
    const insigniaNivel   = $('#insigniaNivel');
    const textoVeredicto  = $('#textoVeredicto');
    const contenedorRadar = $('#contenedorRadar');
    const listaFactores   = $('#listaFactores');

    let intervaloAutoId = null;

    function leerTransaccionFormulario() {
        const [hh] = $('#trHora').value.split(':');
        return {
            cliente: $('#trCliente').value,
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

    async function enviarYEvaluar(datos) {
        try {
            const res = await fetch('/api/v1/transacciones', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos)
            });

            if (!res.ok) throw new Error('Error en el servidor: ' + res.status);
            const resultado = await res.json();
            pintarResultado(resultado);

            if (resultado.nivel === 'critico') {
                lanzarAlerta(resultado);
            }
            return resultado;
        } catch (error) {
            console.error('Error al evaluar transacción:', error);
        }
    }

    function pintarResultado(registro) {
        if (!resultadoVacio || !resultadoLleno) return;
        resultadoVacio.classList.add('d-none');
        resultadoLleno.classList.remove('d-none');

        const { puntaje, nivel, factores, id } = registro;
        const meta = NIVEL_META[nivel] || NIVEL_META.bajo;
        const desc = ESTADO_POR_NIVEL[nivel] || ESTADO_POR_NIVEL.bajo;

        anilloPuntaje.style.setProperty('--p', `${(puntaje / 100) * 360}deg`);
        anilloPuntaje.style.setProperty('--c', meta.color);
        numeroPuntaje.textContent = puntaje;

        insigniaNivel.textContent = `Riesgo ${meta.etiqueta}`;
        insigniaNivel.style.backgroundColor = `${meta.color}22`;
        insigniaNivel.style.color = meta.color;
        insigniaNivel.style.border = `1px solid ${meta.color}`;

        textoVeredicto.innerHTML = `<i class="bi ${desc.icono}" style="color:${desc.color}"></i> ${desc.texto} · ${id}`;

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

        const datoPts = factores.map((f, i) => puntoEnEje(i, f.max > 0 ? (f.puntaje / f.max) : 0).join(',')).join(' ');
        const etiquetasCortas = {
            monto: 'Monto (RF1)',
            dispositivo: 'Disp. (RF2)',
            ip: 'IP (RF3)',
            ubicacion: 'País (RF4)',
            horario: 'Hora (RF5)',
            velocidad: 'Veloc. (RF6)',
            viaje_imposible: 'Viaje (RF7)',
            beneficiario: 'Benef. (RF8)',
            categoria: 'Rubro'
        };

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
            <span class="text-secondary small">${registro.id} · ${registro.clienteNombre} · S/ ${registro.monto.toFixed(2)} · Score: ${registro.puntaje}</span>
        `;
        contenedor.appendChild(div);

        setTimeout(() => {
            div.style.opacity = '0';
            div.style.transition = 'opacity 0.4s ease';
            setTimeout(() => div.remove(), 400);
        }, 5000);
    }

    function generarTransaccionAleatoria() {
        const clientes = ['C-001', 'C-002', 'C-003'];
        const categorias = ['retail', 'restaurante', 'servicios', 'electronica', 'casino', 'cripto'];
        const paises = ['local', 'local', 'local', 'regional', 'alto_riesgo'];

        const cliente = clientes[Math.floor(Math.random() * clientes.length)];
        const opt = form ? form.querySelector(`#trCliente option[value="${cliente}"]`) : null;
        const promedio = opt ? Number(opt.dataset.promedio) : 100;
        const esSospechosa = Math.random() < 0.25;

        return {
            cliente,
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
        if (intervaloAutoId) {
            clearInterval(intervaloAutoId);
            intervaloAutoId = null;
            if (btn) {
                btn.innerHTML = '<i class="bi bi-lightning-charge-fill"></i> Iniciar tráfico automático';
                btn.classList.remove('btn-principal');
                btn.classList.add('btn-secundario');
            }
            return;
        }

        intervaloAutoId = setInterval(() => {
            enviarYEvaluar(generarTransaccionAleatoria());
        }, 1800);

        if (btn) {
            btn.innerHTML = '<i class="bi bi-stop-circle-fill"></i> Detener tráfico automático';
            btn.classList.remove('btn-secundario');
            btn.classList.add('btn-principal');
        }
    }

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            enviarYEvaluar(leerTransaccionFormulario());
        });
    }

    if ($('#btnAuto')) $('#btnAuto').addEventListener('click', alternarTraficoAutomatico);
})();

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
        alto:    { texto: 'Retener / Alertar', icono: 'bi-flag-fill',         color: 'var(--riesgo-alto)' },
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
            const res = await LSApi.fetch('/api/v1/evaluation', {
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

        textoVeredicto.replaceChildren();
        const iconoVeredicto = document.createElement('i');
        iconoVeredicto.className = `bi ${desc.icono}`;
        iconoVeredicto.style.color = desc.color;
        textoVeredicto.append(iconoVeredicto, document.createTextNode(` ${desc.texto} · ${id}`));

        contenedorRadar.replaceChildren(construirRadarSVG(factores));
        listaFactores.replaceChildren(...factores.map(construirFactorElemento));
    }

    function construirFactorElemento(f) {
        const porcentaje = f.max > 0 ? Math.round((f.puntaje / f.max) * 100) : 0;
        let colorBarra = 'var(--riesgo-bajo)';
        if (porcentaje >= 45) colorBarra = 'var(--riesgo-alto)';
        if (porcentaje >= 75) colorBarra = 'var(--riesgo-critico)';

        const columna = document.createElement('div');
        columna.className = 'col-md-6';

        const item = document.createElement('div');
        item.className = 'item-factor';

        const cabecera = document.createElement('div');
        cabecera.className = 'd-flex justify-content-between mb-1';
        cabecera.style.fontSize = '0.85rem';
        cabecera.style.color = 'var(--texto-principal)';

        const etiqueta = document.createElement('span');
        etiqueta.textContent = f.etiqueta;

        const puntaje = document.createElement('strong');
        puntaje.className = 'dato-mono';
        puntaje.textContent = `${f.puntaje}/${f.max}`;

        cabecera.append(etiqueta, puntaje);

        const barra = document.createElement('div');
        barra.className = 'barra-factor';
        const relleno = document.createElement('span');
        relleno.style.width = `${porcentaje}%`;
        relleno.style.background = colorBarra;
        barra.appendChild(relleno);

        const motivo = document.createElement('div');
        motivo.className = 'text-secondary mt-1';
        motivo.style.fontSize = '0.75rem';
        motivo.style.lineHeight = '1.3';
        motivo.textContent = f.motivo;

        item.append(cabecera, barra, motivo);
        columna.appendChild(item);
        return columna;
    }

    function construirRadarSVG(factores) {
        const size = 240, center = size / 2, radius = 84, n = Math.max(1, factores.length);
        const svgNS = 'http://www.w3.org/2000/svg';
        const puntoEnEje = (i, ratio) => {
            const angulo = (Math.PI * 2 * i) / n - Math.PI / 2;
            return [center + Math.cos(angulo) * radius * ratio, center + Math.sin(angulo) * radius * ratio];
        };

        const svg = document.createElementNS(svgNS, 'svg');
        svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
        svg.setAttribute('width', '100%');
        svg.setAttribute('height', '240');

        [0.25, 0.5, 0.75, 1].forEach(r => {
            const polygon = document.createElementNS(svgNS, 'polygon');
            polygon.setAttribute('points', factores.map((_, i) => puntoEnEje(i, r).join(',')).join(' '));
            polygon.setAttribute('fill', 'none');
            polygon.setAttribute('stroke', 'var(--borde-oscuro)');
            polygon.setAttribute('stroke-width', '1');
            svg.appendChild(polygon);
        });

        factores.forEach((_, i) => {
            const [x, y] = puntoEnEje(i, 1);
            const line = document.createElementNS(svgNS, 'line');
            line.setAttribute('x1', center);
            line.setAttribute('y1', center);
            line.setAttribute('x2', x);
            line.setAttribute('y2', y);
            line.setAttribute('stroke', 'var(--borde-oscuro)');
            line.setAttribute('stroke-width', '1');
            svg.appendChild(line);
        });

        const dato = document.createElementNS(svgNS, 'polygon');
        dato.setAttribute('points', factores.map((f, i) => puntoEnEje(i, f.max > 0 ? (f.puntaje / f.max) : 0).join(',')).join(' '));
        dato.setAttribute('fill', 'var(--utp-rojo-brillo)');
        dato.setAttribute('stroke', 'var(--utp-rojo)');
        dato.setAttribute('stroke-width', '2');
        svg.appendChild(dato);

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

        factores.forEach((f, i) => {
            const [x, y] = puntoEnEje(i, 1.22);
            const text = document.createElementNS(svgNS, 'text');
            text.setAttribute('x', x);
            text.setAttribute('y', y);
            text.setAttribute('text-anchor', 'middle');
            text.setAttribute('dominant-baseline', 'middle');
            text.setAttribute('font-size', '10');
            text.setAttribute('fill', 'var(--texto-secundario)');
            text.setAttribute('font-family', 'system-ui');
            text.textContent = etiquetasCortas[f.id] || f.etiqueta;
            svg.appendChild(text);
        });

        return svg;
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

        const titulo = document.createElement('strong');
        titulo.className = 'd-block mb-1 text-danger';
        titulo.style.fontSize = '0.9rem';
        const icono = document.createElement('i');
        icono.className = 'bi bi-shield-x';
        titulo.append(icono, document.createTextNode(' Transacción Bloqueada'));

        const detalle = document.createElement('span');
        detalle.className = 'text-secondary small';
        detalle.textContent = `${registro.id} · ${registro.clienteNombre} · S/ ${Number(registro.monto).toFixed(2)} · Score: ${registro.puntaje}`;

        div.append(titulo, detalle);
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

    function pintarBotonAuto(btn, icono, texto) {
        const i = document.createElement('i');
        i.className = `bi ${icono}`;
        btn.replaceChildren(i, document.createTextNode(` ${texto}`));
    }

    function alternarTraficoAutomatico() {
        const btn = $('#btnAuto');
        if (intervaloAutoId) {
            clearInterval(intervaloAutoId);
            intervaloAutoId = null;
            if (btn) {
                pintarBotonAuto(btn, 'bi-lightning-charge-fill', 'Iniciar tráfico automático');
                btn.classList.remove('btn-principal');
                btn.classList.add('btn-secundario');
            }
            return;
        }

        intervaloAutoId = setInterval(() => {
            enviarYEvaluar(generarTransaccionAleatoria());
        }, 1800);

        if (btn) {
            pintarBotonAuto(btn, 'bi-stop-circle-fill', 'Detener tráfico automático');
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

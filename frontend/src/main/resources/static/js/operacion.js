document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formTransferencia');
    const resultadoDiv = document.getElementById('resultadoTransferencia');
    const btnTransferir = document.getElementById('btnTransferir');

    if (!form) return;

    function crearLineaRecibo(etiqueta, valor) {
        const linea = document.createElement('div');
        const strong = document.createElement('strong');
        strong.textContent = etiqueta;
        linea.append(strong, document.createTextNode(' ' + valor));
        return linea;
    }

    function pintarReciboAprobado(data) {
        const alerta = document.createElement('div');
        alerta.className = 'alert alert-success p-3 rounded';
        alerta.style.borderLeft = '5px solid #146c2e';

        const cabecera = document.createElement('div');
        cabecera.className = 'd-flex align-items-center gap-2 mb-2';
        const icono = document.createElement('i');
        icono.className = 'bi bi-check-circle-fill fs-4 text-success';
        const titulo = document.createElement('strong');
        titulo.className = 'fs-5';
        titulo.textContent = '¡Transferencia Exitosa!';
        cabecera.append(icono, titulo);

        const mensaje = document.createElement('p');
        mensaje.className = 'mb-2 small text-secondary';
        mensaje.textContent = 'Tu dinero ha sido transferido correctamente al destinatario.';

        const detalle = document.createElement('div');
        detalle.className = 'p-2 rounded bg-white border small dato-mono text-dark';
        detalle.append(
            crearLineaRecibo('N° Operación:', data.id),
            crearLineaRecibo('Monto:', 'S/ ' + Number(data.monto).toFixed(2)),
            crearLineaRecibo('Fecha y Hora:', data.fecha),
            crearLineaRecibo('Estado:', 'Aprobada')
        );

        alerta.append(cabecera, mensaje, detalle);
        resultadoDiv.replaceChildren(alerta);
    }

    function pintarReciboRevision(data) {
        const esBloqueo = data.nivel === 'critico';
        const alerta = document.createElement('div');
        alerta.className = 'alert alert-danger p-3 rounded';
        alerta.style.borderLeft = '5px solid var(--utp-rojo)';

        const cabecera = document.createElement('div');
        cabecera.className = 'd-flex align-items-center gap-2 mb-2';
        const icono = document.createElement('i');
        icono.className = 'bi bi-shield-x fs-4 text-danger';
        const titulo = document.createElement('strong');
        titulo.className = 'fs-5';
        titulo.textContent = esBloqueo ? 'Operación Bloqueada por Seguridad' : 'Operación en Validación Preventiva';
        cabecera.append(icono, titulo);

        const mensaje = document.createElement('p');
        mensaje.className = 'mb-2 small';
        mensaje.style.color = 'var(--utp-rojo-oscuro)';
        mensaje.textContent = esBloqueo
            ? 'El motor antifraude detectó indicadores de riesgo crítico y detuvo la operación. El dinero no fue transferido.'
            : 'Hemos detectado parámetros atípicos en esta operación. Por tu seguridad, la transferencia ha sido pausada temporalmente para revisión de identidad.';

        const detalle = document.createElement('div');
        detalle.className = 'p-2 rounded bg-white border small dato-mono text-dark';
        detalle.append(
            crearLineaRecibo('Código de Incidencia:', data.id),
            crearLineaRecibo('Estado:', data.estado),
            crearLineaRecibo('Puntaje de riesgo:', data.puntaje + ' pts')
        );

        const causas = (data.factores || []).filter(f => f.puntaje > 0);
        if (causas.length) {
            const bloque = document.createElement('div');
            bloque.className = 'mt-2 small';
            const tituloCausas = document.createElement('strong');
            tituloCausas.textContent = 'Causas detectadas:';
            bloque.appendChild(tituloCausas);
            const lista = document.createElement('ul');
            lista.className = 'mb-0 mt-1';
            causas.forEach(factor => {
                const li = document.createElement('li');
                li.textContent = factor.etiqueta + ' (' + factor.puntaje + ' / ' + factor.max + ' pts): ' + factor.motivo;
                lista.appendChild(li);
            });
            bloque.appendChild(lista);
            detalle.appendChild(bloque);
        }

        const soporte = document.createElement('p');
        soporte.className = 'mt-2 mb-0 small';
        soporte.textContent = 'Soporte: Comunícate al 0800-LAMBDA si fuiste tú quien solicitó el movimiento.';
        detalle.appendChild(soporte);

        alerta.append(cabecera, mensaje, detalle);
        resultadoDiv.replaceChildren(alerta);
    }

    function pintarError(mensaje) {
        const alerta = document.createElement('div');
        alerta.className = 'alert alert-danger p-3 rounded';
        alerta.style.borderLeft = '5px solid var(--utp-rojo)';
        const cabecera = document.createElement('div');
        cabecera.className = 'd-flex align-items-center gap-2 mb-2';
        const icono = document.createElement('i');
        icono.className = 'bi bi-exclamation-triangle-fill fs-4 text-danger';
        const titulo = document.createElement('strong');
        titulo.className = 'fs-5';
        titulo.textContent = 'No se pudo completar la operación';
        cabecera.append(icono, titulo);
        const texto = document.createElement('p');
        texto.className = 'mb-0 small';
        texto.textContent = mensaje;
        alerta.append(cabecera, texto);
        resultadoDiv.classList.remove('d-none');
        resultadoDiv.replaceChildren(alerta);
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        btnTransferir.disabled = true;
        const spinner = document.createElement('span');
        spinner.className = 'spinner-border spinner-border-sm';
        btnTransferir.replaceChildren(spinner, document.createTextNode(' Verificando con motor antifraude...'));

        const payload = {
            cliente: document.getElementById('trCliente').value,
            monto: Number(document.getElementById('trMonto').value) || 0,
            categoria: document.getElementById('trCategoria').value,
            // Valores de relleno válidos: el servidor deriva hora, frecuencia,
            // dispositivo e IP para usuarios con rol CLIENTE.
            hora: new Date().getHours(),
            pais: document.getElementById('trPais').value,
            dispositivo: 'reconocido',
            operacionesUltimaHora: 1,
            // El servidor deriva hora, frecuencia, dispositivo, IP y el tipo de
            // beneficiario a partir del historial real del cliente.
            beneficiarioNombre: document.getElementById('destinatarioNombre').value.trim(),
            ip: 'conocida'
        };

        try {
            const res = await LSApi.fetch('/api/v1/evaluation', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                let mensaje = 'No se pudo procesar la transferencia.';
                try {
                    const err = await res.json();
                    if (err && err.errores && err.errores.length) mensaje = err.errores[0];
                } catch (e) {
                    // Respuesta sin cuerpo JSON: se conserva el mensaje genérico.
                }
                pintarError(mensaje);
                return;
            }
            const data = await res.json();
            resultadoDiv.classList.remove('d-none');

            if (data.nivel === 'bajo' || data.nivel === 'medio') {
                pintarReciboAprobado(data);
            } else {
                pintarReciboRevision(data);
            }

            // 1. Quitar fila vacía si existe
            const tablaOperaciones = document.getElementById('tablaMisOperaciones');
            if (tablaOperaciones) {
                const trVacia = tablaOperaciones.querySelector('.tr-vacia');
                if (trVacia) trVacia.remove();

                // 2. Crear nueva fila dinámica
                const esAprobada = (data.nivel === 'bajo' || data.nivel === 'medio');

                const diccCat = {
                    servicios: 'Servicios',
                    retail: 'Compras / Retail',
                    restaurante: 'Restaurante / Consumo',
                    electronica: 'Electrónica',
                    casino: 'Apuestas / Juegos',
                    cripto: 'Inversión / Cripto'
                };
                const catTexto = diccCat[payload.categoria] || payload.categoria;

                const tdId = document.createElement('td');
                tdId.className = 'dato-mono fw-bold';
                tdId.textContent = data.id;

                const tdFecha = document.createElement('td');
                tdFecha.textContent = data.fecha;

                const tdMonto = document.createElement('td');
                tdMonto.className = 'dato-mono fw-bold';
                tdMonto.textContent = 'S/ ' + Number(data.monto).toFixed(2);

                const tdCategoria = document.createElement('td');
                tdCategoria.textContent = catTexto;

                const tdEstado = document.createElement('td');
                const badge = document.createElement('span');
                badge.className = 'badge py-1 px-2 ' + (
                    data.nivel === 'bajo' ? 'bg-success'
                        : data.nivel === 'medio' ? 'bg-warning text-dark'
                            : data.nivel === 'alto' ? 'bg-danger' : 'bg-dark');
                const iconoBadge = document.createElement('i');
                iconoBadge.className = 'bi ' + (
                    data.nivel === 'bajo' ? 'bi-check-circle-fill'
                        : data.nivel === 'medio' ? 'bi-eye-fill'
                            : data.nivel === 'alto' ? 'bi-shield-exclamation' : 'bi-x-octagon-fill');
                const textoEstado = data.nivel === 'bajo' ? ' Procesada con éxito'
                    : data.nivel === 'medio' ? ' En monitoreo'
                        : data.nivel === 'alto' ? ' Retenida en revisión' : ' Bloqueada';
                badge.append(iconoBadge, document.createTextNode(textoEstado));
                tdEstado.appendChild(badge);

                const nuevaFila = document.createElement('tr');
                nuevaFila.style.backgroundColor = 'rgba(200, 16, 46, 0.08)';
                nuevaFila.style.transition = 'background-color 1.5s ease';
                nuevaFila.append(tdId, tdFecha, tdMonto, tdCategoria, tdEstado);
                tablaOperaciones.prepend(nuevaFila);
                setTimeout(() => nuevaFila.style.backgroundColor = 'transparent', 1500);

                // 3. Descontar saldo contable si la transacción fue aprobada
                if (esAprobada) {
                    const saldoEl = document.getElementById('saldoDisponibleTexto');
                    if (saldoEl) {
                        let saldoActual = parseFloat(saldoEl.textContent.replace(/,/g, '')) || 4850.00;
                        let nuevoSaldo = Math.max(0, saldoActual - data.monto);
                        saldoEl.textContent = nuevoSaldo.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                    }
                }
            }

            document.getElementById('destinatarioNombre').value = '';
            document.getElementById('cuentaDestino').value = '';
            document.getElementById('trMonto').value = '';

        } catch (err) {
            console.error(err);
            pintarError('Ocurrió un error al procesar la transferencia.');
        } finally {
            btnTransferir.disabled = false;
            const iconoBoton = document.createElement('i');
            iconoBoton.className = 'bi bi-shield-lock';
            btnTransferir.replaceChildren(iconoBoton, document.createTextNode(' Confirmar y Realizar Transferencia'));
        }
    });
});

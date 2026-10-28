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
        const alerta = document.createElement('div');
        alerta.className = 'alert alert-danger p-3 rounded';
        alerta.style.borderLeft = '5px solid var(--utp-rojo)';

        const cabecera = document.createElement('div');
        cabecera.className = 'd-flex align-items-center gap-2 mb-2';
        const icono = document.createElement('i');
        icono.className = 'bi bi-shield-x fs-4 text-danger';
        const titulo = document.createElement('strong');
        titulo.className = 'fs-5';
        titulo.textContent = 'Operación en Validación Preventiva';
        cabecera.append(icono, titulo);

        const mensaje = document.createElement('p');
        mensaje.className = 'mb-2 small';
        mensaje.style.color = 'var(--utp-rojo-oscuro)';
        mensaje.textContent = 'Hemos detectado parámetros atípicos en esta operación (monto inusual o canal no reconocido). Por tu seguridad, la transferencia ha sido pausada temporalmente para revisión de identidad.';

        const detalle = document.createElement('div');
        detalle.className = 'p-2 rounded bg-white border small dato-mono text-dark';
        detalle.append(
            crearLineaRecibo('Código de Incidencia:', data.id),
            crearLineaRecibo('Estado:', 'Retenida en revisión'),
            crearLineaRecibo('Soporte:', 'Comunícate al 0800-LAMBDA si fuiste tú quien solicitó el movimiento.')
        );

        alerta.append(cabecera, mensaje, detalle);
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
            beneficiario: document.getElementById('trBeneficiario').value,
            ip: 'conocida'
        };

        try {
            const res = await LSApi.fetch('/api/v1/evaluation', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('Error en el servidor: ' + res.status);
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
                badge.className = esAprobada ? 'badge bg-success py-1 px-2' : 'badge bg-danger py-1 px-2';
                const iconoBadge = document.createElement('i');
                iconoBadge.className = esAprobada ? 'bi bi-check-circle-fill' : 'bi bi-shield-exclamation';
                badge.append(iconoBadge, document.createTextNode(esAprobada ? ' Procesada con éxito' : ' En validación preventiva'));
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
            alert('Ocurrió un error al procesar la transferencia.');
        } finally {
            btnTransferir.disabled = false;
            const iconoBoton = document.createElement('i');
            iconoBoton.className = 'bi bi-shield-lock';
            btnTransferir.replaceChildren(iconoBoton, document.createTextNode(' Confirmar y Realizar Transferencia'));
        }
    });
});

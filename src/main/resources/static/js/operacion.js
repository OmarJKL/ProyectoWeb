document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formTransferencia');
    const resultadoDiv = document.getElementById('resultadoTransferencia');
    const btnTransferir = document.getElementById('btnTransferir');

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        btnTransferir.disabled = true;
        btnTransferir.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Verificando con motor antifraude...';

        const payload = {
            cliente: document.getElementById('trCliente').value,
            monto: Number(document.getElementById('trMonto').value) || 0,
            categoria: document.getElementById('trCategoria').value,
            hora: Number(document.getElementById('trHora').value) || 12,
            pais: document.getElementById('trPais').value,
            dispositivo: document.getElementById('trDispositivo').value,
            operacionesUltimaHora: Number(document.getElementById('trVelocidad').value) || 1,
            beneficiario: document.getElementById('trBeneficiario').value,
            ip: document.getElementById('trIp').value
        };

        try {
            const res = await fetch('/api/v1/transacciones', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json();
            resultadoDiv.classList.remove('d-none');

            if (data.nivel === 'bajo' || data.nivel === 'medio') {
                resultadoDiv.innerHTML = `
                    <div class="alert alert-success p-3 rounded" style="border-left: 5px solid #146c2e;">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <i class="bi bi-check-circle-fill fs-4 text-success"></i>
                            <strong class="fs-5">¡Transferencia Exitosa!</strong>
                        </div>
                        <p class="mb-2 small text-secondary">Tu dinero ha sido transferido correctamente al destinatario.</p>
                        <div class="p-2 rounded bg-white border small dato-mono text-dark">
                            <div><strong>N° Operación:</strong> ` + data.id + `</div>
                            <div><strong>Monto:</strong> S/ ` + data.monto.toFixed(2) + `</div>
                            <div><strong>Fecha y Hora:</strong> ` + data.fecha + `</div>
                            <div><strong>Estado:</strong> Aprobada</div>
                        </div>
                    </div>
                `;
            } else {
                resultadoDiv.innerHTML = `
                    <div class="alert alert-danger p-3 rounded" style="border-left: 5px solid var(--utp-rojo);">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <i class="bi bi-shield-x fs-4 text-danger"></i>
                            <strong class="fs-5">Operación en Validación Preventiva</strong>
                        </div>
                        <p class="mb-2 small" style="color: var(--utp-rojo-oscuro);">
                            Hemos detectado parámetros atípicos en esta operación (monto inusual o canal no reconocido).
                            Por tu seguridad, la transferencia ha sido pausada temporalmente para revisión de identidad.
                        </p>
                        <div class="p-2 rounded bg-white border small dato-mono text-dark">
                            <div><strong>Código de Incidencia:</strong> ` + data.id + `</div>
                            <div><strong>Estado:</strong> Retenida en revisión</div>
                            <div><strong>Soporte:</strong> Comunícate al 0800-LAMBDA si fuiste tú quien solicitó el movimiento.</div>
                        </div>
                    </div>
                `;
            }

            // 1. Quitar fila vacía si existe
            const tablaOperaciones = document.getElementById('tablaMisOperaciones');
            if (tablaOperaciones) {
                const trVacia = tablaOperaciones.querySelector('.tr-vacia');
                if (trVacia) trVacia.remove();

                // 2. Crear nueva fila dinámica
                const esAprobada = (data.nivel === 'bajo' || data.nivel === 'medio');
                const badgeHtml = esAprobada
                    ? '<span class="badge bg-success py-1 px-2"><i class="bi bi-check-circle-fill"></i> Procesada con éxito</span>'
                    : '<span class="badge bg-danger py-1 px-2"><i class="bi bi-shield-exclamation"></i> En validación preventiva</span>';

                const diccCat = {
                    servicios: 'Servicios',
                    retail: 'Compras / Retail',
                    restaurante: 'Restaurante / Consumo',
                    electronica: 'Electrónica',
                    casino: 'Apuestas / Juegos',
                    cripto: 'Inversión / Cripto'
                };
                const catTexto = diccCat[payload.categoria] || payload.categoria;

                const nuevaFila = document.createElement('tr');
                nuevaFila.style.backgroundColor = 'rgba(200, 16, 46, 0.08)';
                nuevaFila.style.transition = 'background-color 1.5s ease';
                nuevaFila.innerHTML = `
                    <td class="dato-mono fw-bold">${data.id}</td>
                    <td>${data.fecha}</td>
                    <td class="dato-mono fw-bold">S/ ${data.monto.toFixed(2)}</td>
                    <td>${catTexto}</td>
                    <td>${badgeHtml}</td>
                `;
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
            btnTransferir.innerHTML = '<i class="bi bi-shield-lock"></i> Confirmar y Realizar Transferencia';
        }
    });
});

document.addEventListener("DOMContentLoaded", () => {
    const COLOR_BLACK = "#111111";
    const COLOR_RED = "#c8102e";
    const ctx = document.getElementById("transactionsChart");
    let chart;

    async function loadAndRenderChart(rangeDays) {
        try {
            const res = await fetch('/api/v1/dashboard/movimientos?dias=' + rangeDays);
            const data = await res.json();
            const labels = data.map(d => d.fecha);
            const ingresos = data.map(d => d.ingreso);
            const egresos = data.map(d => d.egreso);

            if (chart) {
                chart.data.labels = labels;
                chart.data.datasets[0].data = ingresos;
                chart.data.datasets[1].data = egresos;
                chart.update();
                return;
            }

            if (!ctx) return;
            chart = new Chart(ctx, {
                type: "line",
                data: {
                    labels,
                    datasets: [
                        {
                            label: "Ingresos",
                            data: ingresos,
                            borderColor: COLOR_BLACK,
                            backgroundColor: "rgba(17,17,17,0.06)",
                            borderWidth: 2,
                            pointRadius: 0,
                            pointHoverRadius: 4,
                            tension: 0.35,
                            fill: true,
                        },
                        {
                            label: "Egresos",
                            data: egresos,
                            borderColor: COLOR_RED,
                            backgroundColor: "rgba(200,16,46,0.06)",
                            borderWidth: 2,
                            pointRadius: 0,
                            pointHoverRadius: 4,
                            tension: 0.35,
                            fill: true,
                        },
                    ],
                },
                options: {
                    responsive: true,
                    interaction: { mode: "index", intersect: false },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            backgroundColor: COLOR_BLACK,
                            titleColor: "#ffffff",
                            bodyColor: "#ffffff",
                            padding: 10,
                            cornerRadius: 6,
                            callbacks: {
                                label: (item) => `${item.dataset.label}: S/ ${item.formattedValue}`,
                            },
                        },
                    },
                    scales: {
                        x: {
                            grid: { display: false },
                            ticks: { color: "#6b6b6b", maxRotation: 0, autoSkip: true, maxTicksLimit: 8 },
                        },
                        y: {
                            grid: { color: "#e4e2df" },
                            ticks: { color: "#6b6b6b", callback: (v) => `S/ ${v}` },
                        },
                    },
                },
            });
        } catch (err) {
            console.error("Error al cargar movimientos:", err);
        }
    }

    async function loadAndRenderSummary(rangeDays) {
        try {
            const res = await fetch('/api/v1/dashboard/resumen?dias=' + rangeDays);
            if (!res.ok) return;
            const data = await res.json();

            const elSaldo = document.getElementById('dashSaldoValor');
            const elSaldoDelta = document.getElementById('dashSaldoDelta');
            const elIngresos = document.getElementById('dashIngresosValor');
            const elIngresosDelta = document.getElementById('dashIngresosDelta');
            const elEgresos = document.getElementById('dashEgresosValor');
            const elEgresosDelta = document.getElementById('dashEgresosDelta');

            if (elSaldo) elSaldo.textContent = 'S/ ' + data.saldo.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            if (elSaldoDelta) elSaldoDelta.textContent = '+' + data.variacionSaldo + '% este periodo';
            if (elIngresos) elIngresos.textContent = 'S/ ' + Math.round(data.ingresos).toLocaleString('en-US');
            if (elIngresosDelta) elIngresosDelta.textContent = '+' + data.variacionIngresos + '%';
            if (elEgresos) elEgresos.textContent = 'S/ ' + Math.round(data.egresos).toLocaleString('en-US');
            if (elEgresosDelta) elEgresosDelta.textContent = data.variacionEgresos + '%';
        } catch (err) {
            console.error("Error al actualizar resumen:", err);
        }
    }

    async function loadAndRenderTable(rangeDays) {
        try {
            const res = await fetch('/api/v1/dashboard/recientes?dias=' + rangeDays);
            if (!res.ok) return;
            const movimientos = await res.json();
            const tbody = document.getElementById("transactionsTableBody");
            if (!tbody || !Array.isArray(movimientos)) return;

            tbody.innerHTML = movimientos.map(mov => `
                <tr>
                    <td>${mov.fecha}</td>
                    <td>${mov.descripcion}</td>
                    <td>
                        <span class="lambda-badge ${mov.esIngreso ? 'lambda-badge--ingreso' : 'lambda-badge--egreso'}">
                            ${mov.esIngreso ? 'Ingreso' : 'Egreso'}
                        </span>
                    </td>
                    <td class="text-end ${mov.esIngreso ? 'lambda-amount--positive' : 'lambda-amount--negative'}">
                        ${mov.esIngreso ? '+ ' : '- '}S/ ${mov.monto.toLocaleString('es-PE')}
                    </td>
                </tr>
            `).join('');
        } catch (err) {
            console.error("Error al actualizar tabla:", err);
        }
    }

    let rangoActual = 365;

    const rangeButtons = document.querySelectorAll(".lambda-range-btn");
    rangeButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            rangeButtons.forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            rangoActual = Number(btn.dataset.range);
            loadAndRenderChart(rangoActual);
            loadAndRenderSummary(rangoActual);
            loadAndRenderTable(rangoActual);
        });
    });

    // Carga inicial
    loadAndRenderChart(365);
    loadAndRenderTable(365);

    // Sincronización periódica de la tabla cada 5 segundos
    setInterval(() => {
        loadAndRenderTable(rangoActual);
    }, 5000);
});

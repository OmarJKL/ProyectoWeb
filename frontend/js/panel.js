/**
 * JavaScript Vanilla.
 * Lambda — panel de análisis financiero (gráfico + tabla) — vive solo en el dashboard (index.html).
 *
 * Reemplaza `buildSampleData()` por una llamada a tu API/backend
 * cuando esté lista; la forma de los datos que espera el resto
 * del archivo está documentada justo debajo.
 */

document.addEventListener("DOMContentLoaded", () => {
  const COLOR_BLACK = "#111111";
  const COLOR_RED = "#c8102e";

  /**
   * Genera datos de ejemplo de los últimos 365 días.
   * Cada registro: { date: Date, ingreso: number, egreso: number }
   */
  function buildSampleData() {
    const days = 365;
    const today = new Date();
    const data = [];
    let ingresoBase = 400;
    let egresoBase = 220;

    for (let i = days; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(today.getDate() - i);

      // Variación pseudo-aleatoria pero estable para que la demo se vea natural.
      const seed = Math.sin(i * 0.17) * 120 + Math.cos(i * 0.05) * 80;
      const ingreso = Math.max(50, Math.round(ingresoBase + seed + Math.random() * 60));
      const egreso = Math.max(20, Math.round(egresoBase + seed * 0.6 + Math.random() * 40));

      data.push({ date, ingreso, egreso });
    }
    return data;
  }

  const allData = buildSampleData();

  // ---- Gráfico ----
  const ctx = document.getElementById("transactionsChart");
  let chart;

  function formatLabel(date, rangeDays) {
    const opts = rangeDays <= 30
      ? { day: "2-digit", month: "short" }
      : { month: "short", year: "2-digit" };
    return date.toLocaleDateString("es-PE", opts);
  }

  function sliceByRange(rangeDays) {
    return allData.slice(-rangeDays);
  }

  function renderChart(rangeDays) {
    const slice = sliceByRange(rangeDays);
    const labels = slice.map((d) => formatLabel(d.date, rangeDays));
    const ingresos = slice.map((d) => d.ingreso);
    const egresos = slice.map((d) => d.egreso);

    if (chart) {
      chart.data.labels = labels;
      chart.data.datasets[0].data = ingresos;
      chart.data.datasets[1].data = egresos;
      chart.update();
      return;
    }

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
          legend: { display: false }, // usamos la leyenda HTML propia
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
  }

  // ---- Botones de rango (7d / 15d / 1m / 3m / 1a) ----
  const rangeButtons = document.querySelectorAll(".lambda-range-btn");
  rangeButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      rangeButtons.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      renderChart(Number(btn.dataset.range));
    });
  });

  renderChart(365); // rango inicial: 1 año, igual que el botón activo por defecto

  // ---- Tabla de historial ----
  const tableBody = document.getElementById("transactionsTableBody");

  function renderTable() {
    const recent = allData.slice(-8).reverse();
    const descripciones = [
      "Pago de cliente", "Transferencia recibida", "Compra de insumos",
      "Pago de servicio", "Depósito", "Retiro en cajero",
      "Pago a proveedor", "Venta en tienda",
    ];

    tableBody.innerHTML = recent
      .map((row, i) => {
        const esIngreso = row.ingreso >= row.egreso;
        const monto = esIngreso ? row.ingreso : row.egreso;
        const fecha = row.date.toLocaleDateString("es-PE", {
          day: "2-digit", month: "2-digit", year: "numeric",
        });

        return `
          <tr>
            <td>${fecha}</td>
            <td>${descripciones[i % descripciones.length]}</td>
            <td>
              <span class="lambda-badge ${esIngreso ? "lambda-badge--ingreso" : "lambda-badge--egreso"}">
                ${esIngreso ? "Ingreso" : "Egreso"}
              </span>
            </td>
            <td class="text-end ${esIngreso ? "lambda-amount--positive" : "lambda-amount--negative"}">
              ${esIngreso ? "+" : "-"} S/ ${monto.toLocaleString("es-PE")}
            </td>
          </tr>`;
      })
      .join("");
  }

  renderTable();
});

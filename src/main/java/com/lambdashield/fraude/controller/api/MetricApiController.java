package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.model.response.DashboardResponse;
import com.lambdashield.fraude.model.response.KpisResponse;
import com.lambdashield.fraude.model.response.MovimientoDiarioResponse;
import com.lambdashield.fraude.model.response.MovimientoRecienteResponse;
import com.lambdashield.fraude.model.service.DashboardService;
import com.lambdashield.fraude.model.service.TransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — CAPA CONTROLADOR
 *
 * Recurso: /api/v1/metric
 * Responsabilidad única: exponer métricas y estadísticas del sistema.
 *
 * Cada método cumple exactamente una función:
 *   GET /metric          → KPIs globales (totales, categorías, score promedio)
 *   GET /metric/summary  → resumen financiero por periodo (saldo, ingresos, egresos)
 *   GET /metric/movement → movimientos diarios para el gráfico de líneas
 *   GET /metric/recent   → lista de movimientos recientes para la tabla
 */
@RestController
@RequestMapping("/api/v1/metric")
public class MetricApiController {

    private final TransaccionService transaccionService;
    private final DashboardService dashboardService;

    public MetricApiController(TransaccionService transaccionService,
                               DashboardService dashboardService) {
        this.transaccionService = transaccionService;
        this.dashboardService = dashboardService;
    }

    /** KPIs globales: total de evaluaciones, conteo por nivel y score promedio. */
    @GetMapping
    public ResponseEntity<KpisResponse> kpis() {
        return ResponseEntity.ok(transaccionService.calcularKpis());
    }

    /** Resumen financiero del periodo indicado (saldo, ingresos, egresos y variaciones). */
    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> summary(@RequestParam(defaultValue = "365") int days) {
        return ResponseEntity.ok(dashboardService.obtenerResumen(days));
    }

    /** Serie temporal de movimientos diarios (ingresos/egresos) para el gráfico. */
    @GetMapping("/movement")
    public ResponseEntity<List<MovimientoDiarioResponse>> movement(@RequestParam(defaultValue = "365") int days) {
        return ResponseEntity.ok(dashboardService.generarMovimientos(days));
    }

    /** Movimientos recientes del periodo para la tabla del dashboard. */
    @GetMapping("/recent")
    public ResponseEntity<List<MovimientoRecienteResponse>> recent(@RequestParam(defaultValue = "365") int days) {
        return ResponseEntity.ok(dashboardService.obtenerMovimientosRecientes(days, 8));
    }
}

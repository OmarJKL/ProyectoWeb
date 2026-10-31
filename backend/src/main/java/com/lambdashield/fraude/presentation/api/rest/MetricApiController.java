package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.application.service.DashboardMovimientosService;
import com.lambdashield.fraude.application.service.DashboardResumenService;
import com.lambdashield.fraude.application.service.EvaluacionMetricasService;
import com.lambdashield.fraude.presentation.dto.response.DashboardResponse;
import com.lambdashield.fraude.presentation.dto.response.KpisResponse;
import com.lambdashield.fraude.presentation.dto.response.MovimientoDiarioResponse;
import com.lambdashield.fraude.presentation.dto.response.MovimientoRecienteResponse;
import com.lambdashield.fraude.presentation.mapper.DashboardResponseMapper;
import com.lambdashield.fraude.presentation.mapper.EvaluacionResponseMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para /api/v1/metric: KPIs globales, resumen financiero,
 * serie diaria y movimientos recientes.
 */
@RestController
@RequestMapping("/api/v1/metric")
public class MetricApiController {

    private static final String RANGO_DIAS = "El rango debe estar entre 1 y 365 días";

    private final EvaluacionMetricasService evaluacionMetricasService;
    private final DashboardResumenService dashboardResumenService;
    private final DashboardMovimientosService dashboardMovimientosService;
    private final EvaluacionResponseMapper evaluacionMapper;
    private final DashboardResponseMapper dashboardMapper;

    public MetricApiController(EvaluacionMetricasService evaluacionMetricasService,
                               DashboardResumenService dashboardResumenService,
                               DashboardMovimientosService dashboardMovimientosService,
                               EvaluacionResponseMapper evaluacionMapper,
                               DashboardResponseMapper dashboardMapper) {
        this.evaluacionMetricasService = evaluacionMetricasService;
        this.dashboardResumenService = dashboardResumenService;
        this.dashboardMovimientosService = dashboardMovimientosService;
        this.evaluacionMapper = evaluacionMapper;
        this.dashboardMapper = dashboardMapper;
    }

    /** KPIs globales: total de evaluaciones, conteo por nivel y score promedio. */
    @GetMapping
    public ResponseEntity<KpisResponse> kpis() {
        return ResponseEntity.ok(evaluacionMapper.toResponse(evaluacionMetricasService.calcularKpis()));
    }

    /** Resumen financiero del periodo indicado (saldo, ingresos, egresos y variaciones). */
    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> summary(@RequestParam(defaultValue = "365") @Min(value = 1, message = RANGO_DIAS) @Max(value = 365, message = RANGO_DIAS) int days) {
        return ResponseEntity.ok(dashboardMapper.toResponse(dashboardResumenService.obtenerResumen(days)));
    }

    /** Serie temporal de movimientos diarios (ingresos/egresos) para el gráfico. */
    @GetMapping("/movement")
    public ResponseEntity<List<MovimientoDiarioResponse>> movement(@RequestParam(defaultValue = "365") @Min(value = 1, message = RANGO_DIAS) @Max(value = 365, message = RANGO_DIAS) int days) {
        return ResponseEntity.ok(dashboardMovimientosService.generarMovimientos(days)
                .stream().map(dashboardMapper::toResponse).toList());
    }

    /** Movimientos recientes del periodo para la tabla del dashboard. */
    @GetMapping("/recent")
    public ResponseEntity<List<MovimientoRecienteResponse>> recent(@RequestParam(defaultValue = "365") @Min(value = 1, message = RANGO_DIAS) @Max(value = 365, message = RANGO_DIAS) int days) {
        return ResponseEntity.ok(dashboardMovimientosService.obtenerMovimientosRecientes(days, 8)
                .stream().map(dashboardMapper::toResponse).toList());
    }
}

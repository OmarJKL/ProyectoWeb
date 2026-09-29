package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.dto.response.DashboardResponse;
import com.lambdashield.fraude.dto.response.MovimientoDiarioResponse;
import com.lambdashield.fraude.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoDiarioResponse>> movimientos(@RequestParam(defaultValue = "365") int dias) {
        return ResponseEntity.ok(dashboardService.generarMovimientos(dias));
    }

    @GetMapping("/resumen")
    public ResponseEntity<DashboardResponse> resumen(@RequestParam(defaultValue = "365") int dias) {
        return ResponseEntity.ok(dashboardService.obtenerResumen(dias));
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<com.lambdashield.fraude.dto.response.MovimientoRecienteResponse>> recientes(@RequestParam(defaultValue = "365") int dias) {
        return ResponseEntity.ok(dashboardService.obtenerMovimientosRecientes(dias, 8));
    }
}

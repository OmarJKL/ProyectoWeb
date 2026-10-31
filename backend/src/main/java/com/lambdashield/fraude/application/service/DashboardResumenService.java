package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.ResumenFinanciero;
import com.lambdashield.fraude.application.port.DashboardDataSource;
import org.springframework.stereotype.Service;

import java.util.List;

/** Caso de uso para el resumen financiero mostrado en el dashboard. */
@Service
public class DashboardResumenService {

    private final EvaluacionConsultaService evaluacionConsultaService;
    private final DashboardDataSource dataSource;

    public DashboardResumenService(EvaluacionConsultaService evaluacionConsultaService,
                                   DashboardDataSource dataSource) {
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.dataSource = dataSource;
    }

    public ResumenFinanciero obtenerResumen() {
        return obtenerResumen(365);
    }

    public ResumenFinanciero obtenerResumen(int dias) {
        ResumenFinanciero base = dataSource.resumenBase(dias);
        double egresos = base.egresos();

        List<EvaluacionResultado> transacciones = evaluacionConsultaService.listarTodas();
        double egresosTransacciones = transacciones.stream()
                .filter(t -> "bajo".equals(t.nivel()) || "medio".equals(t.nivel()))
                .mapToDouble(EvaluacionResultado::monto)
                .sum();
        egresos += Math.round(egresosTransacciones);

        return new ResumenFinanciero(
                base.saldo() + base.ingresos() - egresos,
                base.ingresos(),
                egresos,
                base.variacionSaldo(),
                base.variacionIngresos(),
                base.variacionEgresos()
        );
    }
}

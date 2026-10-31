package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.EvaluacionResultado;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteResumenService {

    private final EvaluacionConsultaService evaluacionConsultaService;

    public record ResumenReporte(
            long total,
            long aprobadas,
            long monitoreadas,
            long enRevision,
            long bloqueadas,
            double montoTotal,
            double montoBloqueado,
            double tasaBloqueo
    ) {}

    public ReporteResumenService(EvaluacionConsultaService evaluacionConsultaService) {
        this.evaluacionConsultaService = evaluacionConsultaService;
    }

    public ResumenReporte obtenerResumen() {
        List<EvaluacionResultado> transacciones = evaluacionConsultaService.listarTodas();
        long total = transacciones.size();
        long aprobadas = transacciones.stream().filter(t -> "bajo".equals(t.nivel())).count();
        long monitoreadas = transacciones.stream().filter(t -> "medio".equals(t.nivel())).count();
        long enRevision = transacciones.stream().filter(t -> "alto".equals(t.nivel())).count();
        long bloqueadas = transacciones.stream().filter(t -> "critico".equals(t.nivel())).count();

        double montoTotal = transacciones.stream().mapToDouble(EvaluacionResultado::monto).sum();
        double montoBloqueado = transacciones.stream()
                .filter(t -> "critico".equals(t.nivel()) || "alto".equals(t.nivel()))
                .mapToDouble(EvaluacionResultado::monto).sum();

        double tasaBloqueo = total > 0 ? ((double) bloqueadas / total) * 100.0 : 0.0;

        return new ResumenReporte(total, aprobadas, monitoreadas, enRevision, bloqueadas,
                montoTotal, montoBloqueado, tasaBloqueo);
    }
}

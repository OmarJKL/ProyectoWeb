package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.store.EvaluacionStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteService {

    private final EvaluacionStore evaluacionStore;

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

    public ReporteService(EvaluacionStore evaluacionStore) {
        this.evaluacionStore = evaluacionStore;
    }

    public ResumenReporte obtenerResumen() {
        List<EvaluacionResponse> transacciones = evaluacionStore.listarTodas();
        long total = transacciones.size();
        long aprobadas = transacciones.stream().filter(t -> "bajo".equals(t.nivel())).count();
        long monitoreadas = transacciones.stream().filter(t -> "medio".equals(t.nivel())).count();
        long enRevision = transacciones.stream().filter(t -> "alto".equals(t.nivel())).count();
        long bloqueadas = transacciones.stream().filter(t -> "critico".equals(t.nivel())).count();

        double montoTotal = transacciones.stream().mapToDouble(EvaluacionResponse::monto).sum();
        double montoBloqueado = transacciones.stream()
                .filter(t -> "critico".equals(t.nivel()) || "alto".equals(t.nivel()))
                .mapToDouble(EvaluacionResponse::monto).sum();

        double tasaBloqueo = total > 0 ? ((double) bloqueadas / total) * 100.0 : 0.0;

        return new ResumenReporte(
                total,
                aprobadas,
                monitoreadas,
                enRevision,
                bloqueadas,
                montoTotal,
                montoBloqueado,
                tasaBloqueo
        );
    }

    public byte[] exportarTransaccionesCsv() {
        List<EvaluacionResponse> transacciones = evaluacionStore.listarTodas();
        StringBuilder sb = new StringBuilder();

        // Encabezados CSV
        sb.append("ID,Fecha,Titular,Monto (S/),Categoria,Ubicacion,Hora,Puntaje,Nivel,Estado\n");

        for (EvaluacionResponse t : transacciones) {
            sb.append(String.format(Locale.US,
                    "\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",%02d:00,%d,\"%s\",\"%s\"\n",
                    t.id(),
                    t.fecha(),
                    t.clienteNombre(),
                    t.monto(),
                    t.categoria(),
                    t.pais(),
                    t.hora(),
                    t.puntaje(),
                    t.nivel().toUpperCase(),
                    t.estado()
            ));
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}

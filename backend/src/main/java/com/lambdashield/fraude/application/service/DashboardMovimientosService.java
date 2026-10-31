package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.MovimientoDiario;
import com.lambdashield.fraude.application.model.MovimientoReciente;
import com.lambdashield.fraude.application.port.DashboardDataSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Provee los movimientos resumidos usados por las gráficas y tablas del dashboard. */
@Service
public class DashboardMovimientosService {

    private final EvaluacionConsultaService evaluacionConsultaService;
    private final DashboardDataSource dataSource;

    public DashboardMovimientosService(EvaluacionConsultaService evaluacionConsultaService,
                                       DashboardDataSource dataSource) {
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.dataSource = dataSource;
    }

    public List<MovimientoDiario> generarMovimientos(int dias) {
        return dataSource.movimientosDiarios(dias);
    }

    public List<MovimientoReciente> obtenerMovimientosRecientes(int cantidad) {
        return obtenerMovimientosRecientes(365, cantidad);
    }

    public List<MovimientoReciente> obtenerMovimientosRecientes(int dias, int cantidad) {
        List<MovimientoReciente> resultado = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (EvaluacionResultado transaccion : evaluacionConsultaService.listarTodas().stream().limit(4).toList()) {
            resultado.add(new MovimientoReciente(
                    hoy.format(formato),
                    "Transferencia (" + transaccion.id() + ") · " + transaccion.clienteNombre(),
                    false,
                    (int) Math.round(transaccion.monto())
            ));
        }

        dataSource.completarMovimientosRecientes(resultado, dias, cantidad, hoy);
        return resultado;
    }
}

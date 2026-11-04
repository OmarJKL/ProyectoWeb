package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.MovimientoDiario;
import com.lambdashield.fraude.application.model.MovimientoReciente;
import com.lambdashield.fraude.domain.entity.Transaccion;
import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provee los movimientos reales del periodo usados por las gráficas y tablas
 * del dashboard: serie diaria de ingresos/egresos y últimos movimientos.
 */
@Service
public class DashboardMovimientosService {

    private final TransaccionRepository transaccionRepository;

    public DashboardMovimientosService(TransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional(readOnly = true)
    public List<MovimientoDiario> generarMovimientos(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate desdeFecha = hoy.minusDays(dias);
        List<Transaccion> transacciones = transaccionesDelPeriodo(desdeFecha, hoy);

        Map<LocalDate, double[]> porDia = new LinkedHashMap<>();
        for (LocalDate dia = desdeFecha; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            porDia.put(dia, new double[2]);
        }
        for (Transaccion transaccion : transacciones) {
            LocalDate dia = transaccion.getFechaHoraTransaccion().toLocalDate();
            double[] acumulado = porDia.get(dia);
            if (acumulado == null) {
                continue;
            }
            if (transaccion.getTipoTransaccion().getNaturaleza() == NaturalezaTransaccion.CREDITO) {
                acumulado[0] += transaccion.getMontoTransaccion().doubleValue();
            } else {
                acumulado[1] += transaccion.getMontoTransaccion().doubleValue();
            }
        }

        DateTimeFormatter formato = dias <= 30
                ? DateTimeFormatter.ofPattern("dd/MM")
                : DateTimeFormatter.ofPattern("dd/MM/yy");
        List<MovimientoDiario> serie = new ArrayList<>();
        porDia.forEach((dia, totales) -> serie.add(new MovimientoDiario(
                dia.format(formato), (int) Math.round(totales[0]), (int) Math.round(totales[1]))));
        return serie;
    }

    @Transactional(readOnly = true)
    public List<MovimientoReciente> obtenerMovimientosRecientes(int cantidad) {
        return obtenerMovimientosRecientes(365, cantidad);
    }

    @Transactional(readOnly = true)
    public List<MovimientoReciente> obtenerMovimientosRecientes(int dias, int cantidad) {
        LocalDate hoy = LocalDate.now();
        List<Transaccion> transacciones = transaccionesDelPeriodo(hoy.minusDays(dias), hoy);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return transacciones.stream()
                .limit(cantidad)
                .map(transaccion -> new MovimientoReciente(
                        transaccion.getFechaHoraTransaccion().format(formato),
                        transaccion.getTipoTransaccion().getNombreTipo() + " · "
                                + transaccion.getCuenta().getCliente().getCodigoCliente(),
                        transaccion.getTipoTransaccion().getNaturaleza() == NaturalezaTransaccion.CREDITO,
                        (int) Math.round(transaccion.getMontoTransaccion().doubleValue())
                ))
                .toList();
    }

    private List<Transaccion> transaccionesDelPeriodo(LocalDate desdeFecha, LocalDate hastaFecha) {
        OffsetDateTime desde = desdeFecha.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime hasta = hastaFecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        return transaccionRepository.buscarDelPeriodo(desde, hasta);
    }
}

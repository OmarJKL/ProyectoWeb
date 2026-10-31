package com.lambdashield.fraude.config.demo;

import com.lambdashield.fraude.application.model.MovimientoDiario;
import com.lambdashield.fraude.application.model.MovimientoReciente;
import com.lambdashield.fraude.application.model.ResumenFinanciero;
import com.lambdashield.fraude.application.port.DashboardDataSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación demo del puerto {@link DashboardDataSource}: cifras ilustrativas
 * que se reemplazan por consultas agregadas reales cuando existan.
 */
@Component
public class DashboardDemoDataProvider implements DashboardDataSource {

    private static final String[] DESCRIPCIONES = {
            "Pago de cliente", "Transferencia recibida", "Compra de insumos",
            "Pago de servicio", "Depósito", "Retiro en cajero",
            "Pago a proveedor", "Venta en tienda"
    };

    @Override
    public ResumenFinanciero resumenBase(int dias) {
        return switch (dias) {
            case 7 -> new ResumenFinanciero(15690.50, 2850.0, 1320.0, 3.2, 8.5, -1.2);
            case 15 -> new ResumenFinanciero(15690.50, 5420.0, 2650.0, 4.8, 9.7, -2.1);
            case 30 -> new ResumenFinanciero(15690.50, 8960.0, 4120.0, 6.1, 10.4, -2.8);
            case 90 -> new ResumenFinanciero(15690.50, 12300.0, 5600.0, 7.5, 11.2, -3.1);
            default -> new ResumenFinanciero(15690.50, 15420.0, 6930.0, 8.4, 12.1, -3.6);
        };
    }

    @Override
    public List<MovimientoDiario> movimientosDiarios(int dias) {
        List<MovimientoDiario> resultado = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter formato = dias <= 30
                ? DateTimeFormatter.ofPattern("dd/MM")
                : DateTimeFormatter.ofPattern("dd/MM/yy");
        for (int i = dias; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            double variacion = Math.sin(i * 0.17) * 120.0 + Math.cos(i * 0.05) * 80.0;
            int ingreso = (int) Math.max(50, Math.round(400 + variacion + (i % 7) * 8.5));
            int egreso = (int) Math.max(20, Math.round(220 + variacion * 0.6 + (i % 5) * 7.2));
            resultado.add(new MovimientoDiario(fecha.format(formato), ingreso, egreso));
        }
        return resultado;
    }

    @Override
    public void completarMovimientosRecientes(List<MovimientoReciente> movimientos,
                                              int dias, int cantidad, LocalDate hoy) {
        int restantes = Math.max(0, cantidad - movimientos.size());
        int paso = Math.max(1, dias / 8);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < restantes; i++) {
            int diasAtras = Math.min(dias, (i + 1) * paso);
            LocalDate fecha = hoy.minusDays(diasAtras);
            String descripcion = DESCRIPCIONES[i % DESCRIPCIONES.length];
            boolean esIngreso = i % 2 == 0;
            int monto = esIngreso ? 450 + i * 110 : 220 + i * 85;
            movimientos.add(new MovimientoReciente(fecha.format(formato), descripcion, esIngreso, monto));
        }
    }
}

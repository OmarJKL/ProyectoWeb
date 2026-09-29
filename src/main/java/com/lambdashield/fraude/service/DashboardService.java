package com.lambdashield.fraude.service;

import com.lambdashield.fraude.dto.response.DashboardResponse;
import com.lambdashield.fraude.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.dto.response.MovimientoDiarioResponse;
import com.lambdashield.fraude.dto.response.MovimientoRecienteResponse;
import com.lambdashield.fraude.store.EvaluacionStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    private final EvaluacionStore evaluacionStore;

    private static final String[] DESCRIPCIONES = {
            "Pago de cliente", "Transferencia recibida", "Compra de insumos",
            "Pago de servicio", "Depósito", "Retiro en cajero",
            "Pago a proveedor", "Venta en tienda"
    };

    public DashboardService(EvaluacionStore evaluacionStore) {
        this.evaluacionStore = evaluacionStore;
    }

    public DashboardResponse obtenerResumen() {
        return obtenerResumen(365);
    }

    public DashboardResponse obtenerResumen(int dias) {
        // Cuadre contable exacto: Saldo Actual = Saldo Base + Ingresos - Egresos
        double saldoBase = 15690.50;
        double ingresos;
        double egresos;
        double varSaldo;
        double varIngresos;
        double varEgresos;

        switch (dias) {
            case 7 -> {
                ingresos = 2850.0;
                egresos = 1320.0;
                varSaldo = 3.2;
                varIngresos = 8.5;
                varEgresos = -1.2;
            }
            case 15 -> {
                ingresos = 5420.0;
                egresos = 2650.0;
                varSaldo = 4.8;
                varIngresos = 9.7;
                varEgresos = -2.1;
            }
            case 30 -> {
                ingresos = 8960.0;
                egresos = 4120.0;
                varSaldo = 6.1;
                varIngresos = 10.4;
                varEgresos = -2.8;
            }
            case 90 -> {
                ingresos = 12300.0;
                egresos = 5600.0;
                varSaldo = 7.5;
                varIngresos = 11.2;
                varEgresos = -3.1;
            }
            default -> { // 365 días / Anual
                ingresos = 15420.0;
                egresos = 6930.0;
                varSaldo = 8.4;
                varIngresos = 12.1;
                varEgresos = -3.6;
            }
        }

        // Incorporar impacto de las operaciones de clientes evaluadas
        List<EvaluacionResponse> transacciones = evaluacionStore.listarTodas();
        double egresosTransacciones = transacciones.stream()
                .filter(t -> "bajo".equals(t.nivel()) || "medio".equals(t.nivel()))
                .mapToDouble(EvaluacionResponse::monto)
                .sum();
        egresos += Math.round(egresosTransacciones);

        double saldoActual = saldoBase + ingresos - egresos;

        return new DashboardResponse(
                saldoActual,
                ingresos,
                egresos,
                varSaldo,
                varIngresos,
                varEgresos
        );
    }

    public List<MovimientoDiarioResponse> generarMovimientos(int dias) {
        List<MovimientoDiarioResponse> resultado = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        int ingresoBase = 400;
        int egresoBase = 220;
        DateTimeFormatter formatter = dias <= 30
                ? DateTimeFormatter.ofPattern("dd/MM")
                : DateTimeFormatter.ofPattern("dd/MM/yy");

        for (int i = dias; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            double seed = Math.sin(i * 0.17) * 120.0 + Math.cos(i * 0.05) * 80.0;
            int ingreso = (int) Math.max(50, Math.round(ingresoBase + seed + (i % 7) * 8.5));
            int egreso = (int) Math.max(20, Math.round(egresoBase + seed * 0.6 + (i % 5) * 7.2));
            resultado.add(new MovimientoDiarioResponse(fecha.format(formatter), ingreso, egreso));
        }
        return resultado;
    }

    public List<MovimientoRecienteResponse> obtenerMovimientosRecientes(int cantidad) {
        return obtenerMovimientosRecientes(365, cantidad);
    }

    public List<MovimientoRecienteResponse> obtenerMovimientosRecientes(int dias, int cantidad) {
        List<MovimientoRecienteResponse> resultado = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate hoy = LocalDate.now();

        // 1. Incorporar operaciones reales evaluadas de los clientes
        List<EvaluacionResponse> txs = evaluacionStore.listarTodas();
        int agregadosReales = 0;
        for (EvaluacionResponse tx : txs) {
            if (agregadosReales >= 4) break;
            boolean aprobada = "bajo".equals(tx.nivel()) || "medio".equals(tx.nivel());
            String desc = "Transferencia (" + tx.id() + ") · " + tx.clienteNombre();
            resultado.add(new MovimientoRecienteResponse(
                    hoy.format(fmt),
                    desc,
                    false, // Es egreso del titular
                    (int) Math.round(tx.monto())
            ));
            agregadosReales++;
        }

        // 2. Completar con movimientos del periodo histórico seleccionado
        int restantes = Math.max(0, cantidad - resultado.size());
        int paso = Math.max(1, dias / 8);

        for (int i = 0; i < restantes; i++) {
            int diasAtras = Math.min(dias, (i + 1) * paso);
            LocalDate fecha = hoy.minusDays(diasAtras);
            String desc = DESCRIPCIONES[i % DESCRIPCIONES.length];
            boolean esIngreso = (i % 2 == 0);
            int monto = esIngreso ? 450 + (i * 110) : 220 + (i * 85);
            resultado.add(new MovimientoRecienteResponse(fecha.format(fmt), desc, esIngreso, monto));
        }

        return resultado;
    }
}

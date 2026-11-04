package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.ResumenFinanciero;
import com.lambdashield.fraude.domain.enums.EstadoCuenta;
import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import com.lambdashield.fraude.repository.CuentaRepository;
import com.lambdashield.fraude.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Caso de uso para el resumen financiero mostrado en el dashboard.
 * Todas las cifras provienen de PostgreSQL: saldo de las cuentas activas e
 * ingresos/egresos según la naturaleza de cada transacción del periodo.
 */
@Service
public class DashboardResumenService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;

    public DashboardResumenService(TransaccionRepository transaccionRepository,
                                   CuentaRepository cuentaRepository) {
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public ResumenFinanciero obtenerResumen() {
        return obtenerResumen(365);
    }

    @Transactional(readOnly = true)
    public ResumenFinanciero obtenerResumen(int dias) {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime desde = hoy.minusDays(dias).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime hasta = hoy.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime desdePeriodoAnterior = desde.minusDays(dias + 1L);

        double ingresos = aDouble(transaccionRepository.sumarMonto(desde, hasta, NaturalezaTransaccion.CREDITO));
        double egresos = aDouble(transaccionRepository.sumarMonto(desde, hasta, NaturalezaTransaccion.DEBITO));
        double ingresosAnteriores = aDouble(
                transaccionRepository.sumarMonto(desdePeriodoAnterior, desde, NaturalezaTransaccion.CREDITO));
        double egresosAnteriores = aDouble(
                transaccionRepository.sumarMonto(desdePeriodoAnterior, desde, NaturalezaTransaccion.DEBITO));

        double saldo = aDouble(cuentaRepository.sumarSaldoPorEstado(EstadoCuenta.ACTIVA));
        double saldoInicio = saldo - ingresos + egresos;

        return new ResumenFinanciero(
                saldo,
                ingresos,
                egresos,
                variacionPorcentual(saldo, saldoInicio),
                variacionPorcentual(ingresos, ingresosAnteriores),
                variacionPorcentual(egresos, egresosAnteriores)
        );
    }

    /** Variación porcentual respecto al periodo anterior; 0 si no hay base de comparación. */
    static double variacionPorcentual(double actual, double anterior) {
        if (anterior == 0) {
            return actual == 0 ? 0 : 100;
        }
        return Math.round(((actual - anterior) / Math.abs(anterior)) * 1000.0) / 10.0;
    }

    private static double aDouble(BigDecimal valor) {
        return valor != null ? valor.doubleValue() : 0.0;
    }
}

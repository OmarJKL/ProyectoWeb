package com.lambdashield.fraude.application.port;

import com.lambdashield.fraude.application.model.MovimientoDiario;
import com.lambdashield.fraude.application.model.MovimientoReciente;
import com.lambdashield.fraude.application.model.ResumenFinanciero;

import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de salida para las cifras del dashboard que aún no provienen de la base de datos.
 * La implementación vive en la capa de configuración y puede sustituirse por consultas reales.
 */
public interface DashboardDataSource {

    /** Saldo base, ingresos, egresos y variaciones del periodo indicado, sin incluir evaluaciones. */
    ResumenFinanciero resumenBase(int dias);

    List<MovimientoDiario> movimientosDiarios(int dias);

    void completarMovimientosRecientes(List<MovimientoReciente> movimientos, int dias, int cantidad, LocalDate hoy);
}
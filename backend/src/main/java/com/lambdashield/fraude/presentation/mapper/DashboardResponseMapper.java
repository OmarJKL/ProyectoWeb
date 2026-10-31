package com.lambdashield.fraude.presentation.mapper;

import com.lambdashield.fraude.application.model.MovimientoDiario;
import com.lambdashield.fraude.application.model.MovimientoReciente;
import com.lambdashield.fraude.application.model.ResumenFinanciero;
import com.lambdashield.fraude.presentation.dto.response.DashboardResponse;
import com.lambdashield.fraude.presentation.dto.response.MovimientoDiarioResponse;
import com.lambdashield.fraude.presentation.dto.response.MovimientoRecienteResponse;
import org.springframework.stereotype.Component;

/** Traduce los modelos del dashboard a los DTO HTTP. */
@Component
public class DashboardResponseMapper {

    public DashboardResponse toResponse(ResumenFinanciero resumen) {
        return new DashboardResponse(
                resumen.saldo(),
                resumen.ingresos(),
                resumen.egresos(),
                resumen.variacionSaldo(),
                resumen.variacionIngresos(),
                resumen.variacionEgresos()
        );
    }

    public MovimientoDiarioResponse toResponse(MovimientoDiario movimiento) {
        return new MovimientoDiarioResponse(movimiento.fecha(), movimiento.ingreso(), movimiento.egreso());
    }

    public MovimientoRecienteResponse toResponse(MovimientoReciente movimiento) {
        return new MovimientoRecienteResponse(
                movimiento.fecha(), movimiento.descripcion(), movimiento.esIngreso(), movimiento.monto());
    }
}

package com.lambdashield.fraude.application.mapper;

import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.FactorResultado;
import com.lambdashield.fraude.domain.customer.NombreClienteFormatter;
import com.lambdashield.fraude.domain.entity.Cliente;
import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.domain.entity.EvaluacionRegla;
import com.lambdashield.fraude.domain.entity.ReglaFraude;
import com.lambdashield.fraude.domain.entity.Transaccion;
import com.lambdashield.fraude.domain.fraud.FactorEvaluado;
import com.lambdashield.fraude.domain.fraud.FactorRegla;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Convierte las entidades y resultados del dominio en el modelo de aplicación. */
@Component
public class EvaluacionResultadoMapper {

    public EvaluacionResultado toResultado(EvaluacionFraude evaluacion, List<FactorResultado> factores) {
        Transaccion transaccion = evaluacion.getTransaccion();
        Cliente cliente = transaccion.getCuenta().getCliente();

        return new EvaluacionResultado(
                String.format("TR-%05d", evaluacion.getIdEvaluacion()),
                cliente.getCodigoCliente(),
                NombreClienteFormatter.nombreCompleto(cliente),
                transaccion.getMontoTransaccion().doubleValue(),
                transaccion.getTipoTransaccion().getCodigoTipo(),
                paisDesdeCodigo(transaccion.getCodigoPais()),
                transaccion.getFechaHoraTransaccion().getHour(),
                evaluacion.getFraudeScore().intValue(),
                evaluacion.getNivelRiesgo().name().toLowerCase(Locale.ROOT),
                evaluacion.getFechaEvaluacion().toLocalDateTime(),
                factores,
                evaluacion.getIdEvaluacion()
        );
    }

    public FactorResultado toFactorResultado(FactorEvaluado factor) {
        return new FactorResultado(factor.id(), factor.etiqueta(), factor.puntaje(), factor.max(), factor.motivo());
    }

    public FactorResultado toFactor(EvaluacionRegla evaluacionRegla) {
        ReglaFraude regla = evaluacionRegla.getRegla();
        int maximo = regla.getPeso() != null ? regla.getPeso().intValue() : 0;
        int puntaje = evaluacionRegla.getPesoAplicado() != null
                ? evaluacionRegla.getPesoAplicado().intValue()
                : 0;
        return new FactorResultado(
                FactorRegla.porCodigo(regla.getCodigoRegla())
                        .map(FactorRegla::id)
                        .orElse(regla.getCodigoRegla().toLowerCase(Locale.ROOT)),
                regla.getNombreRegla(),
                puntaje,
                maximo,
                evaluacionRegla.getValorDetectado()
        );
    }

    private static String paisDesdeCodigo(String codigo) {
        if ("PE".equals(codigo)) return "local";
        if ("CL".equals(codigo) || "CO".equals(codigo) || "EC".equals(codigo)) return "regional";
        return "alto_riesgo";
    }
}

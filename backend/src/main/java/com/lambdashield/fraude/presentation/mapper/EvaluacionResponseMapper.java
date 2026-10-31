package com.lambdashield.fraude.presentation.mapper;

import com.lambdashield.fraude.application.model.EvaluacionResultado;
import com.lambdashield.fraude.application.model.FactorResultado;
import com.lambdashield.fraude.application.model.KpisResultado;
import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.presentation.dto.response.FactorResponse;
import com.lambdashield.fraude.presentation.dto.response.KpisResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Traduce los resultados de aplicación a los DTO HTTP, incluidas las etiquetas visuales. */
@Component
public class EvaluacionResponseMapper {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EvaluacionResponse toResponse(EvaluacionResultado resultado) {
        String nivel = resultado.nivel();
        return new EvaluacionResponse(
                resultado.id(),
                resultado.clienteCodigo(),
                resultado.clienteNombre(),
                resultado.monto(),
                resultado.categoria(),
                resultado.pais(),
                resultado.hora(),
                resultado.puntaje(),
                nivel,
                estado(nivel),
                icono(nivel),
                color(nivel),
                resultado.fechaEvaluacion().format(FORMATO_FECHA),
                resultado.fechaEvaluacion(),
                resultado.factores().stream().map(this::toFactor).toList(),
                resultado.idInterno()
        );
    }

    public List<EvaluacionResponse> toResponses(List<EvaluacionResultado> resultados) {
        return resultados.stream().map(this::toResponse).toList();
    }

    public FactorResponse toFactor(FactorResultado factor) {
        return new FactorResponse(factor.id(), factor.etiqueta(), factor.puntaje(), factor.max(), factor.motivo());
    }

    public KpisResponse toResponse(KpisResultado kpis) {
        return new KpisResponse(kpis.total(), kpis.aprobadas(), kpis.monitoreadas(),
                kpis.enRevision(), kpis.bloqueadas(), kpis.puntajePromedio());
    }

    private static String estado(String nivel) {
        return switch (nivel) {
            case "bajo" -> "Aprobada";
            case "medio" -> "Monitoreo";
            case "alto" -> "Retener / Alertar";
            default -> "Bloqueada";
        };
    }

    private static String icono(String nivel) {
        return switch (nivel) {
            case "bajo" -> "bi-check-circle-fill";
            case "medio" -> "bi-eye-fill";
            case "alto" -> "bi-flag-fill";
            default -> "bi-x-octagon-fill";
        };
    }

    private static String color(String nivel) {
        return switch (nivel) {
            case "bajo" -> "var(--riesgo-bajo)";
            case "medio" -> "var(--riesgo-medio)";
            case "alto" -> "var(--riesgo-alto)";
            default -> "var(--riesgo-critico)";
        };
    }
}

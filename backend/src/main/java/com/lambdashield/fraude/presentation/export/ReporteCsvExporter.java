package com.lambdashield.fraude.presentation.export;

import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Serializa el historial de evaluaciones al formato CSV que descarga la interfaz. */
@Component
public class ReporteCsvExporter {

    public byte[] exportar(List<EvaluacionResponse> filas) {
        StringBuilder csv = new StringBuilder(
                "ID,Fecha,Titular,Monto (S/),Categoria,Ubicacion,Hora,Puntaje,Nivel,Estado\n");
        for (EvaluacionResponse evaluacion : filas) {
            csv.append(campo(evaluacion.id())).append(',')
                    .append(campo(evaluacion.fecha())).append(',')
                    .append(campo(evaluacion.clienteNombre())).append(',')
                    .append(String.format(Locale.US, "%.2f", evaluacion.monto())).append(',')
                    .append(campo(evaluacion.categoria())).append(',')
                    .append(campo(evaluacion.pais())).append(',')
                    .append(String.format(Locale.ROOT, "%02d:00", evaluacion.hora())).append(',')
                    .append(evaluacion.puntaje()).append(',')
                    .append(campo(evaluacion.nivel().toUpperCase(Locale.ROOT))).append(',')
                    .append(campo(evaluacion.estado())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String campo(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\"", "\"\"")) + "\"";
    }
}

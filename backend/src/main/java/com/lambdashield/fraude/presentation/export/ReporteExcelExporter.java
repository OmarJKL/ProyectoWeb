package com.lambdashield.fraude.presentation.export;

import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Genera el reporte de evaluaciones en formato Excel (.xlsx) con Apache POI. */
@Component
public class ReporteExcelExporter {

    private static final String[] ENCABEZADO = {
            "ID", "Fecha", "Titular", "Monto (S/)", "Categoría", "Ubicación",
            "Hora", "Puntaje", "Nivel", "Estado"
    };

    public byte[] exportar(List<EvaluacionResponse> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            XSSFSheet hoja = libro.createSheet("Evaluaciones");

            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADO.length; i++) {
                cabecera.createCell(i).setCellValue(ENCABEZADO[i]);
            }

            int fila = 1;
            for (EvaluacionResponse evaluacion : filas) {
                Row datos = hoja.createRow(fila++);
                crearCelda(datos, 0, evaluacion.id());
                crearCelda(datos, 1, evaluacion.fecha());
                crearCelda(datos, 2, evaluacion.clienteNombre());
                crearCelda(datos, 3, String.format(Locale.US, "%.2f", evaluacion.monto()));
                crearCelda(datos, 4, evaluacion.categoria());
                crearCelda(datos, 5, evaluacion.pais());
                crearCelda(datos, 6, String.format(Locale.ROOT, "%02d:00", evaluacion.hora()));
                crearCelda(datos, 7, String.valueOf(evaluacion.puntaje()));
                crearCelda(datos, 8, evaluacion.nivel().toUpperCase(Locale.ROOT));
                crearCelda(datos, 9, evaluacion.estado());
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel", e);
        }
    }

    private static void crearCelda(Row fila, int indice, String valor) {
        Cell celda = fila.createCell(indice);
        celda.setCellValue(valor);
    }
}

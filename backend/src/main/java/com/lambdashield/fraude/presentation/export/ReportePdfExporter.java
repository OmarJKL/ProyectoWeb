package com.lambdashield.fraude.presentation.export;

import com.lambdashield.fraude.presentation.dto.response.EvaluacionResponse;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

/** Genera el reporte de evaluaciones en formato PDF con OpenPDF. */
@Component
public class ReportePdfExporter {

    private static final String[] ENCABEZADO = {
            "ID", "Fecha", "Titular", "Monto (S/)", "Categoría", "Ubicación",
            "Hora", "Puntaje", "Nivel", "Estado"
    };

    public byte[] exportar(List<EvaluacionResponse> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph titulo = new Paragraph("Reporte de evaluaciones antifraude",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            titulo.setSpacingAfter(12f);
            documento.add(titulo);

            PdfPTable tabla = new PdfPTable(ENCABEZADO.length);
            tabla.setWidthPercentage(100);
            for (String columna : ENCABEZADO) {
                PdfPCell celda = new PdfPCell(new Paragraph(columna,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                tabla.addCell(celda);
            }

            for (EvaluacionResponse evaluacion : filas) {
                tabla.addCell(evaluacion.id());
                tabla.addCell(evaluacion.fecha());
                tabla.addCell(evaluacion.clienteNombre());
                tabla.addCell(String.format(Locale.US, "%.2f", evaluacion.monto()));
                tabla.addCell(evaluacion.categoria());
                tabla.addCell(evaluacion.pais());
                tabla.addCell(String.format(Locale.ROOT, "%02d:00", evaluacion.hora()));
                tabla.addCell(String.valueOf(evaluacion.puntaje()));
                tabla.addCell(evaluacion.nivel().toUpperCase(Locale.ROOT));
                tabla.addCell(evaluacion.estado());
            }
            documento.add(tabla);

            Paragraph pie = new Paragraph("Total de registros: " + filas.size(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8));
            pie.setAlignment(Element.ALIGN_RIGHT);
            pie.setSpacingBefore(10f);
            documento.add(pie);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }
}

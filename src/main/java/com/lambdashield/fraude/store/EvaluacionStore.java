package com.lambdashield.fraude.store;

import com.lambdashield.fraude.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.dto.response.FactorResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EvaluacionStore {
    private final CopyOnWriteArrayList<EvaluacionResponse> evaluaciones = new CopyOnWriteArrayList<>();
    private final AtomicLong contador = new AtomicLong(1);

    public String generarId() {
        return String.format("TR-%05d", contador.getAndIncrement());
    }

    public void guardar(EvaluacionResponse evaluacion) {
        evaluaciones.add(evaluacion);
    }

    public List<EvaluacionResponse> listarTodas() {
        List<EvaluacionResponse> reversa = new ArrayList<>(evaluaciones);
        Collections.reverse(reversa);
        return reversa;
    }

    @jakarta.annotation.PostConstruct
    public void inicializarDatosPrueba() {
        if (!evaluaciones.isEmpty()) return;

        // Factores base reutilizables
        List<FactorResponse> factoresBajo = List.of(
                new FactorResponse("monto", "RF001 · Monto inusual", 0, 25, "Monto habitual dentro del promedio."),
                new FactorResponse("dispositivo", "RF002 · Dispositivo nuevo", 0, 15, "Dispositivo reconocido."),
                new FactorResponse("ip", "RF003 · IP nueva", 0, 10, "IP habitual."),
                new FactorResponse("ubicacion", "RF004 · País nuevo", 0, 30, "Ubicación local."),
                new FactorResponse("horario", "RF005 · Horario inusual", 0, 10, "Horario diurno normal."),
                new FactorResponse("velocidad", "RF006 · Alta frecuencia", 0, 25, "1 operación en la última hora."),
                new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", 0, 40, "Desplazamiento normal."),
                new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", 0, 20, "Destinatario habitual.")
        );

        // 1. C-001 María Fernández (Aprobada - Bajo)
        guardar(new EvaluacionResponse(generarId(), "María Fernández", 150.00, "servicios", "local", 14,
                8, "bajo", "Aprobada", "bi-check-circle-fill", "var(--riesgo-bajo)", "28/09/2026 14:20", factoresBajo));

        // 2. C-001 María Fernández (Aprobada - Bajo)
        guardar(new EvaluacionResponse(generarId(), "María Fernández", 85.50, "retail", "local", 11,
                12, "bajo", "Aprobada", "bi-check-circle-fill", "var(--riesgo-bajo)", "28/09/2026 18:45", factoresBajo));

        // 3. C-002 Jorge Salas (Aprobada - Bajo)
        guardar(new EvaluacionResponse(generarId(), "Jorge Salas", 320.00, "electronica", "local", 16,
                15, "bajo", "Aprobada", "bi-check-circle-fill", "var(--riesgo-bajo)", "28/09/2026 19:10", factoresBajo));

        // 4. C-003 Ana Quiroz (Aprobada - Bajo)
        guardar(new EvaluacionResponse(generarId(), "Ana Quiroz", 95.00, "restaurante", "local", 13,
                6, "bajo", "Aprobada", "bi-check-circle-fill", "var(--riesgo-bajo)", "29/09/2026 09:30", factoresBajo));

        // 5. C-001 María Fernández (Aprobada - Bajo)
        guardar(new EvaluacionResponse(generarId(), "María Fernández", 210.00, "servicios", "local", 10,
                10, "bajo", "Aprobada", "bi-check-circle-fill", "var(--riesgo-bajo)", "29/09/2026 10:15", factoresBajo));

        // 6. C-002 Jorge Salas (Monitoreo - Medio)
        List<FactorResponse> factoresMedio1 = List.of(
                new FactorResponse("dispositivo", "RF002 · Dispositivo nuevo", 15, 15, "Dispositivo no reconocido."),
                new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", 20, 20, "Destinatario nuevo.")
        );
        guardar(new EvaluacionResponse(generarId(), "Jorge Salas", 750.00, "retail", "local", 15,
                35, "medio", "Monitoreo", "bi-eye-fill", "var(--riesgo-medio)", "29/09/2026 11:00", factoresMedio1));

        // 7. C-003 Ana Quiroz (Monitoreo - Medio)
        List<FactorResponse> factoresMedio2 = List.of(
                new FactorResponse("horario", "RF005 · Horario inusual", 10, 10, "Operación nocturna."),
                new FactorResponse("ip", "RF003 · IP nueva", 10, 10, "IP no habitual."),
                new FactorResponse("monto", "RF001 · Monto inusual", 25, 25, "Monto por encima del promedio.")
        );
        guardar(new EvaluacionResponse(generarId(), "Ana Quiroz", 600.00, "electronica", "local", 23,
                45, "medio", "Monitoreo", "bi-eye-fill", "var(--riesgo-medio)", "29/09/2026 11:35", factoresMedio2));

        // 8. C-002 Jorge Salas (Retención / Alerta - Alto)
        List<FactorResponse> factoresAlto1 = List.of(
                new FactorResponse("ubicacion", "RF004 · País nuevo", 30, 30, "Operación desde país regional vecino."),
                new FactorResponse("velocidad", "RF006 · Alta frecuencia", 20, 25, "4 transacciones en < 1 hora."),
                new FactorResponse("ip", "RF003 · IP nueva", 10, 10, "IP nueva no registrada.")
        );
        guardar(new EvaluacionResponse(generarId(), "Jorge Salas", 1850.00, "servicios", "regional", 14,
                65, "alto", "Retener / Alertar", "bi-flag-fill", "var(--riesgo-alto)", "29/09/2026 12:10", factoresAlto1));

        // 9. C-003 Ana Quiroz (Retención / Alerta - Alto)
        List<FactorResponse> factoresAlto2 = List.of(
                new FactorResponse("monto", "RF001 · Monto inusual", 25, 25, "Monto 6x superior a promedio."),
                new FactorResponse("beneficiario", "RF008 · Beneficiario nuevo + monto alto", 20, 20, "Destinatario nuevo con monto alto."),
                new FactorResponse("dispositivo", "RF002 · Dispositivo nuevo", 15, 15, "Dispositivo nuevo."),
                new FactorResponse("horario", "RF005 · Horario inusual", 10, 10, "Madrugada.")
        );
        guardar(new EvaluacionResponse(generarId(), "Ana Quiroz", 3200.00, "cripto", "local", 3,
                70, "alto", "Retener / Alertar", "bi-flag-fill", "var(--riesgo-alto)", "29/09/2026 12:45", factoresAlto2));

        // 10. C-002 Jorge Salas (Bloqueo Crítico - Crítico)
        List<FactorResponse> factoresCritico = List.of(
                new FactorResponse("viaje_imposible", "RF007 · Viaje imposible", 40, 40, "Distancia geográfica incompatible en < 1 hora."),
                new FactorResponse("ubicacion", "RF004 · País nuevo", 30, 30, "Jurisdicción internacional de alto riesgo."),
                new FactorResponse("velocidad", "RF006 · Alta frecuencia", 25, 25, "7 operaciones en < 1 hora."),
                new FactorResponse("monto", "RF001 · Monto inusual", 25, 25, "Monto desproporcionado.")
        );
        guardar(new EvaluacionResponse(generarId(), "Jorge Salas", 8900.00, "casino", "alto_riesgo", 2,
                100, "critico", "Bloqueada", "bi-x-octagon-fill", "var(--riesgo-critico)", "29/09/2026 13:00", factoresCritico));
    }

    public List<EvaluacionResponse> listarPorCliente(String clienteNombre) {
        return listarTodas().stream()
                .filter(e -> e.clienteNombre().toLowerCase().contains(clienteNombre.toLowerCase()))
                .toList();
    }

    public void limpiar() {
        evaluaciones.clear();
        contador.set(1);
    }
}

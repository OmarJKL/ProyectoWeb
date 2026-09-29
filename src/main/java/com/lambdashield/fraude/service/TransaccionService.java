package com.lambdashield.fraude.service;

import com.lambdashield.fraude.dto.request.TransaccionCrearRequest;
import com.lambdashield.fraude.dto.response.EvaluacionResponse;
import com.lambdashield.fraude.dto.response.FactorResponse;
import com.lambdashield.fraude.dto.response.KpisResponse;
import com.lambdashield.fraude.store.ClienteStore;
import com.lambdashield.fraude.store.EvaluacionStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransaccionService {
    private final DeteccionFraudeService deteccionFraudeService;
    private final EvaluacionStore evaluacionStore;
    private final ClienteStore clienteStore;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TransaccionService(DeteccionFraudeService deteccionFraudeService,
                              EvaluacionStore evaluacionStore,
                              ClienteStore clienteStore) {
        this.deteccionFraudeService = deteccionFraudeService;
        this.evaluacionStore = evaluacionStore;
        this.clienteStore = clienteStore;
    }

    public EvaluacionResponse crearYEvaluar(TransaccionCrearRequest request) {
        ClienteStore.ClienteInfo clienteInfo = clienteStore.buscar(request.cliente());
        double promedio = clienteInfo != null ? clienteInfo.promedioGasto() : 100.0;
        String nombreCliente = clienteInfo != null ? clienteInfo.nombre() : request.cliente();

        List<FactorResponse> factores = deteccionFraudeService.evaluar(request, promedio);
        int puntaje = deteccionFraudeService.calcularPuntajeTotal(factores);
        String nivel = deteccionFraudeService.clasificarNivel(puntaje);
        String estado = deteccionFraudeService.determinarEstado(nivel);
        String icono = deteccionFraudeService.obtenerIconoEstado(nivel);
        String color = deteccionFraudeService.obtenerColorNivel(nivel);
        String fecha = LocalDateTime.now().format(FORMATTER);
        String id = evaluacionStore.generarId();

        EvaluacionResponse response = new EvaluacionResponse(
                id,
                nombreCliente,
                request.monto(),
                request.categoria(),
                request.pais(),
                request.hora(),
                puntaje,
                nivel,
                estado,
                icono,
                color,
                fecha,
                factores
        );

        evaluacionStore.guardar(response);
        return response;
    }

    public List<EvaluacionResponse> listarTodas() {
        return evaluacionStore.listarTodas();
    }

    public List<EvaluacionResponse> listarPorCliente(String clienteNombre) {
        return evaluacionStore.listarPorCliente(clienteNombre);
    }

    public KpisResponse calcularKpis() {
        List<EvaluacionResponse> todas = evaluacionStore.listarTodas();
        long total = todas.size();
        long aprobadas = todas.stream().filter(e -> "bajo".equals(e.nivel())).count();
        long monitoreadas = todas.stream().filter(e -> "medio".equals(e.nivel())).count();
        long enRevision = todas.stream().filter(e -> "alto".equals(e.nivel())).count();
        long bloqueadas = todas.stream().filter(e -> "critico".equals(e.nivel())).count();
        double promedio = total > 0
                ? todas.stream().mapToInt(EvaluacionResponse::puntaje).average().orElse(0.0)
                : 0.0;
        return new KpisResponse(total, aprobadas, monitoreadas, enRevision, bloqueadas, promedio);
    }

    public void limpiarHistorial() {
        evaluacionStore.limpiar();
    }
}

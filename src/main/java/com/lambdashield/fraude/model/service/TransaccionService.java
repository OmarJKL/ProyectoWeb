package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.request.TransaccionCrearRequest;
import com.lambdashield.fraude.model.response.EvaluacionResponse;
import com.lambdashield.fraude.model.response.FactorResponse;
import com.lambdashield.fraude.model.response.KpisResponse;
import com.lambdashield.fraude.model.store.ClienteStore;
import com.lambdashield.fraude.model.store.EvaluacionStore;
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
        ClienteStore.ClienteInfo clienteInfo = obtenerCliente(request.cliente());
        return crearYEvaluar(request, LocalDateTime.now(), clienteInfo);
    }

    /**
     * Evaluación para el portal del cliente: hora, frecuencia, dispositivo e IP
     * se derivan en el servidor y no se confía en los valores del body.
     */
    public EvaluacionResponse crearYEvaluarCliente(TransaccionCrearRequest request, String codigoCliente,
                                                   String userAgent, String ipRemota) {
        ClienteStore.ClienteInfo clienteInfo = obtenerCliente(codigoCliente);
        LocalDateTime fechaHora = LocalDateTime.now();

        int operacionesUltimaHora = evaluacionStore.contarOperacionesUltimaHora(
                codigoCliente, fechaHora.minusHours(1));

        String dispositivo = clienteStore.esDispositivoConocido(codigoCliente, userAgent)
                ? "reconocido"
                : "nuevo";
        String ip = clienteStore.esIpConocida(codigoCliente, ipRemota)
                ? "conocida"
                : "nueva";

        TransaccionCrearRequest derivada = new TransaccionCrearRequest(
                codigoCliente,
                request.monto(),
                request.categoria(),
                fechaHora.getHour(),
                request.pais(),
                dispositivo,
                operacionesUltimaHora,
                request.beneficiario(),
                ip
        );

        return crearYEvaluar(derivada, fechaHora, clienteInfo);
    }

    private ClienteStore.ClienteInfo obtenerCliente(String codigoCliente) {
        ClienteStore.ClienteInfo clienteInfo = clienteStore.buscar(codigoCliente);
        if (clienteInfo == null) {
            throw new ClienteNoEncontradoException(codigoCliente);
        }
        return clienteInfo;
    }

    private EvaluacionResponse crearYEvaluar(TransaccionCrearRequest request, LocalDateTime fechaHora,
                                             ClienteStore.ClienteInfo clienteInfo) {
        double promedio = clienteInfo.promedioGasto();
        String nombreCliente = clienteInfo.nombre();

        List<FactorResponse> factores = deteccionFraudeService.evaluar(request, promedio);
        int puntaje = deteccionFraudeService.calcularPuntajeTotal(factores);
        String nivel = deteccionFraudeService.clasificarNivel(puntaje);
        String estado = deteccionFraudeService.determinarEstado(nivel);
        String icono = deteccionFraudeService.obtenerIconoEstado(nivel);
        String color = deteccionFraudeService.obtenerColorNivel(nivel);
        String fecha = fechaHora.format(FORMATTER);
        String id = evaluacionStore.generarId();

        EvaluacionResponse response = new EvaluacionResponse(
                id,
                request.cliente(),
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
                fechaHora,
                factores
        );

        evaluacionStore.guardar(response);
        return response;
    }

    public List<EvaluacionResponse> listarTodas() {
        return evaluacionStore.listarTodas();
    }

    public List<EvaluacionResponse> listarPorClienteCodigo(String codigoCliente) {
        return evaluacionStore.listarPorClienteCodigo(codigoCliente);
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

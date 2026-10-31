package com.lambdashield.fraude.config.seed;

import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.application.command.EvaluarTransaccionCommand;
import com.lambdashield.fraude.repository.EvaluacionFraudeRepository;
import com.lambdashield.fraude.application.service.EvaluacionTransaccionService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Genera transacciones iniciales solo en bases vacías, reutilizando el caso de uso real. */
@Component
public class EvaluacionDemoSeeder {

    private record DemoTransaccion(String cliente, double monto, String categoria, int hora,
                                   String pais, String dispositivo, int operaciones,
                                   String beneficiario, String ip) {}

    private static final List<DemoTransaccion> DEMOS = List.of(
            new DemoTransaccion("C-001", 150.00, "servicios", 14, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-001", 85.50, "retail", 9, "local", "reconocido", 1, "recurrente", "conocida"),
            new DemoTransaccion("C-002", 320.00, "electronica", 18, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-003", 95.00, "restaurante", 20, "local", "reconocido", 0, "recurrente", "conocida"),
            new DemoTransaccion("C-001", 210.00, "retail", 11, "regional", "nuevo", 2, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 750.00, "electronica", 2, "local", "reconocido", 3, "recurrente", "conocida"),
            new DemoTransaccion("C-003", 600.00, "servicios", 3, "regional", "nuevo", 4, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 1850.00, "cripto", 1, "alto_riesgo", "nuevo", 5, "nuevo", "nueva"),
            new DemoTransaccion("C-003", 3200.00, "casino", 4, "alto_riesgo", "nuevo", 6, "nuevo", "nueva"),
            new DemoTransaccion("C-002", 8900.00, "cripto", 0, "alto_riesgo", "nuevo", 6, "nuevo", "nueva")
    );

    private final EvaluacionFraudeRepository evaluacionRepository;
    private final EvaluacionTransaccionService evaluacionTransaccionService;

    public EvaluacionDemoSeeder(EvaluacionFraudeRepository evaluacionRepository,
                                EvaluacionTransaccionService evaluacionTransaccionService) {
        this.evaluacionRepository = evaluacionRepository;
        this.evaluacionTransaccionService = evaluacionTransaccionService;
    }

    public void seed() {
        if (evaluacionRepository.count() > 0) return;
        for (DemoTransaccion demo : DEMOS) {
            evaluacionTransaccionService.crearYEvaluar(new EvaluarTransaccionCommand(
                    demo.cliente(), demo.monto(), demo.categoria(), demo.hora(), demo.pais(),
                    demo.dispositivo(), demo.operaciones(), demo.beneficiario(), demo.ip()));
        }
    }
}

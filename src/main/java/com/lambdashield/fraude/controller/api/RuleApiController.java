package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.model.response.ReglaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — CAPA CONTROLADOR
 *
 * Recurso: /api/v1/rule
 * Responsabilidad única: exponer las reglas del motor antifraude (RF001–RF008).
 *
 * Este controller es la fuente de verdad de las reglas.
 * La vista /reglas (ReglaViewController) también referencia esta misma lista.
 */
@RestController
@RequestMapping("/api/v1/rule")
public class RuleApiController {

    /**
     * Lista canónica de reglas del motor antifraude.
     * Pesos y descripciones alineados al documento de arquitectura (Punto 18).
     */
    public static final List<ReglaResponse> RULES = List.of(
            new ReglaResponse("RF001", "Unusual Amount",
                    "Condition: amount > 5x average_30d. Penalizes operations that drastically exceed the holder's historical consumption.",
                    25),
            new ReglaResponse("RF002", "New Device",
                    "Condition: is_new_device = true. Detects access from phones or browsers without a previously registered fingerprint.",
                    15),
            new ReglaResponse("RF003", "New IP",
                    "Condition: is_new_ip = true. Connections to digital banking from IP addresses that do not match the usual network.",
                    10),
            new ReglaResponse("RF004", "New Country",
                    "Condition: is_new_country = true. Penalizes transfers or operations originating from infrequent countries or jurisdictions.",
                    30),
            new ReglaResponse("RF005", "Unusual Hours",
                    "Condition: is_unusual_hour = true. The early morning window (00:00–05:00) concentrates higher probabilistic risk.",
                    10),
            new ReglaResponse("RF006", "High Frequency",
                    "Condition: transactions_last_hour > 5. Fast bursts of operations typical of cloning or card testing.",
                    25),
            new ReglaResponse("RF007", "Impossible Travel",
                    "Condition: distance / time exceeds physical threshold. Detects geographic jumps incompatible within short intervals.",
                    40),
            new ReglaResponse("RF008", "New Beneficiary + High Amount",
                    "Condition: new_beneficiary && amount > threshold. Recipient with no history associated with a high transfer amount.",
                    20)
    );

    /** Devuelve la lista completa de reglas del motor antifraude. */
    @GetMapping
    public ResponseEntity<List<ReglaResponse>> list() {
        return ResponseEntity.ok(RULES);
    }
}

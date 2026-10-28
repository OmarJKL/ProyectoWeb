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
                    "Gradual score by amount / average_30d ratio: ≤1.5x = 0; >1.5x and ≤3x = 5–15; >3x and <5x = 15–22; ≥5x = 25. Penalizes operations that drastically exceed the holder's historical consumption.",
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
                    "Condition: is_unusual_hour = true. Only the early morning window (00:00–04:59) scores; the rest of the day scores 0.",
                    10),
            new ReglaResponse("RF006", "High Frequency",
                    "Gradual score by transactions_last_hour: ≤1 = 0; 2–3 = 5–12; 4–5 = 15–20; >5 = 25. Fast bursts of operations typical of cloning or card testing.",
                    25),
            new ReglaResponse("RF007", "Impossible Travel",
                    "Simplified heuristic based on country, transaction frequency, and IP (approximation pending real geolocation).",
                    40),
            new ReglaResponse("RF008", "New Beneficiary + High Amount",
                    "Condition: new_beneficiary && amount > threshold. Scores 0 when the beneficiary is not new or the amount is not high.",
                    20)
    );

    /** Devuelve la lista completa de reglas del motor antifraude. */
    @GetMapping
    public ResponseEntity<List<ReglaResponse>> list() {
        return ResponseEntity.ok(RULES);
    }
}

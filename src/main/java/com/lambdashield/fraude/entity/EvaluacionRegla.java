package com.lambdashield.fraude.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluacion_regla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionRegla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion_regla")
    private Long idEvaluacionRegla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionFraude evaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_regla", nullable = false)
    private ReglaFraude regla;

    @Column(name = "se_disparo", nullable = false)
    private boolean seDisparo;

    @Column(name = "valor_detectado", length = 250)
    private String valorDetectado;

    @Column(name = "peso_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoAplicado;

    @Column(name = "detalle_json", columnDefinition = "text")
    private String detalleJson;
}

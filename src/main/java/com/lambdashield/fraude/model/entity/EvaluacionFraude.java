package com.lambdashield.fraude.model.entity;

import com.lambdashield.fraude.model.enums.DecisionFraude;
import com.lambdashield.fraude.model.enums.NivelRiesgo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "evaluacion_fraude")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionFraude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaccion", nullable = false)
    private Transaccion transaccion;

    @Column(name = "fraude_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal fraudeScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private NivelRiesgo nivelRiesgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private DecisionFraude decision;

    @Column(name = "es_posible_fraude", nullable = false)
    private boolean esPosibleFraude;

    @Column(name = "modelo_utilizado", length = 100)
    private String modeloUtilizado;

    @Column(name = "version_modelo", length = 40)
    private String versionModelo;

    @Column(name = "fecha_evaluacion", nullable = false)
    private OffsetDateTime fechaEvaluacion;

    @Column(name = "duracion_ms")
    private Integer duracionMs;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<EvaluacionRegla> reglasEvaluadas;

    @OneToMany(mappedBy = "evaluacion")
    @ToString.Exclude
    private List<AlertaFraude> alertas;
}

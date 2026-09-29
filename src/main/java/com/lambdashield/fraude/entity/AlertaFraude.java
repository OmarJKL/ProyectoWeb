package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.EstadoAlerta;
import com.lambdashield.fraude.enums.NivelRiesgo;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerta_fraude")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaFraude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long idAlerta;

    @Column(name = "codigo_alerta", nullable = false, unique = true)
    private UUID codigoAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaccion", nullable = false)
    private Transaccion transaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionFraude evaluacion;

    @Column(name = "tipo_alerta", nullable = false, length = 50)
    private String tipoAlerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_prioridad", nullable = false, length = 20)
    private NivelRiesgo nivelPrioridad;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_alerta", nullable = false, length = 30)
    private EstadoAlerta estadoAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_asignado")
    private UsuarioSistema usuarioAsignado;

    @Column(name = "fecha_generacion", nullable = false)
    private OffsetDateTime fechaGeneracion;

    @Column(name = "fecha_revision")
    private OffsetDateTime fechaRevision;
}

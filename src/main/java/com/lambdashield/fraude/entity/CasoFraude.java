package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.EstadoCaso;
import com.lambdashield.fraude.enums.ResultadoCaso;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "caso_fraude")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasoFraude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso")
    private Long idCaso;

    @Column(name = "codigo_caso", nullable = false, unique = true)
    private UUID codigoCaso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta", nullable = false)
    private AlertaFraude alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responsable")
    private UsuarioSistema usuarioResponsable;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_caso", nullable = false, length = 30)
    private EstadoCaso estadoCaso;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_caso", length = 30)
    private ResultadoCaso resultadoCaso;

    @Column(name = "comentario_cierre", columnDefinition = "text")
    private String comentarioCierre;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @OneToMany(mappedBy = "caso", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<HistorialCaso> historial;
}

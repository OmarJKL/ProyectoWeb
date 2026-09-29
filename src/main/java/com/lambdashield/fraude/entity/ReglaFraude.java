package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.NivelRiesgo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "regla_fraude")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaFraude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer idRegla;

    @Column(name = "codigo_regla", nullable = false, unique = true, length = 30)
    private String codigoRegla;

    @Column(name = "nombre_regla", nullable = false, length = 150)
    private String nombreRegla;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "peso", nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "parametros_json", columnDefinition = "text")
    private String parametrosJson;

    @Column(name = "version_regla", nullable = false)
    private int versionRegla;

    @Column(name = "es_activa", nullable = false)
    private boolean esActiva;

    @Column(name = "fecha_inicio_vigencia", nullable = false)
    private OffsetDateTime fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private OffsetDateTime fechaFinVigencia;
}

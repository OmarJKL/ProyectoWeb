package com.lambdashield.fraude.domain.entity;

import com.lambdashield.fraude.domain.enums.NivelRiesgo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(name = "codigo_cliente", nullable = false, unique = true, length = 20)
    private String codigoCliente;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "nombres", length = 100)
    private String nombres;

    @Column(name = "apellido_paterno", length = 100)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "correo_electronico", length = 150)
    private String correoElectronico;

    @Column(name = "num", length = 30)
    private String numeroTelefono;

    @Column(name = "promedio_gasto", nullable = false, precision = 18, scale = 2)
    private BigDecimal promedioGasto;

    @Column(name = "es_activo", nullable = false)
    private boolean esActivo;

    // Campos calculados o de catálogo que no forman parte del esquema base.
    @Transient
    private String codigoPaisResidencia;

    @Transient
    private NivelRiesgo nivelRiesgoCliente;

    @Transient
    private OffsetDateTime fechaCreacion;

    @Transient
    private OffsetDateTime fechaActualizacion;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Cuenta> cuentas;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Dispositivo> dispositivos;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Beneficiario> beneficiarios;
}

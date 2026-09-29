package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.TipoDispositivo;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dispositivo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispositivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Long idDispositivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "huella_dispositivo", unique = true, length = 255)
    private String huellaDispositivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dispositivo", length = 30)
    private TipoDispositivo tipoDispositivo;

    @Column(name = "sistema_operativo", length = 50)
    private String sistemaOperativo;

    @Column(name = "version_sistema", length = 30)
    private String versionSistema;

    @Column(name = "navegador", length = 50)
    private String navegador;

    @Column(name = "es_conocido", nullable = false)
    private boolean esConocido;

    @Column(name = "fecha_primer_uso")
    private OffsetDateTime fechaPrimerUso;

    @Column(name = "fecha_ultimo_uso")
    private OffsetDateTime fechaUltimoUso;
}

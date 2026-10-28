package com.lambdashield.fraude.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioSistema usuario;

    @Column(name = "entidad", nullable = false, length = 80)
    private String entidad;

    @Column(name = "id_entidad", nullable = false, length = 80)
    private String idEntidad;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "direccion_ip", columnDefinition = "inet")
    private String direccionIp;

    @Column(name = "fecha_evento", nullable = false)
    private OffsetDateTime fechaEvento;
}

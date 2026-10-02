package com.lambdashield.fraude.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "codigo_rol", nullable = false, unique = true, length = 40)
    private String codigoRol;

    @Column(name = "nombre_rol", nullable = false, length = 100)
    private String nombreRol;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Column(name = "es_activo", nullable = false)
    private boolean esActivo;
}

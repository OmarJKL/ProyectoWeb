package com.lambdashield.fraude.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "canal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Canal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canal")
    private Short idCanal;

    @Column(name = "codigo_canal", nullable = false, unique = true, length = 20)
    private String codigoCanal;

    @Column(name = "nombre_canal", nullable = false, length = 100)
    private String nombreCanal;

    @Column(name = "es_activo")
    private Boolean esActivo;
}

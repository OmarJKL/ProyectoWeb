package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.NaturalezaTransaccion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTransaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_transaccion")
    private Integer idTipoTransaccion;

    @Column(name = "codigo_tipo", nullable = false, unique = true, length = 40)
    private String codigoTipo;

    @Column(name = "nombre_tipo", nullable = false, length = 120)
    private String nombreTipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false, length = 10)
    private NaturalezaTransaccion naturaleza;

    @Column(name = "es_activo", nullable = false)
    private boolean esActivo;
}

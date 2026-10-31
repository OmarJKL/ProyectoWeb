package com.lambdashield.fraude.domain.entity;

import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_transaccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTransaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_transaccion")
    private Short idTipoTransaccion;

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

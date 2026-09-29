package com.lambdashield.fraude.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "caracteristica_transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaracteristicaTransaccion {
    @Id
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_transaccion")
    @ToString.Exclude
    private Transaccion transaccion;

    @Column(name = "monto_promedio_30d", precision = 18, scale = 2)
    private BigDecimal montoPromedio30d;

    @Column(name = "monto_maximo_30d", precision = 18, scale = 2)
    private BigDecimal montoMaximo30d;

    @Column(name = "cantidad_transacciones_10m", nullable = false)
    private int cantidadTransacciones10m;

    @Column(name = "cantidad_transacciones_1h", nullable = false)
    private int cantidadTransacciones1h;

    @Column(name = "cantidad_transacciones_24h", nullable = false)
    private int cantidadTransacciones24h;

    @Column(name = "cantidad_destinatarios_24h", nullable = false)
    private int cantidadDestinatarios24h;

    @Column(name = "minutos_desde_ultima_transaccion")
    private Integer minutosDesdeUltimaTransaccion;

    @Column(name = "distancia_ultima_transaccion_km", precision = 10, scale = 2)
    private BigDecimal distanciaUltimaTransaccionKm;

    @Column(name = "es_dispositivo_nuevo", nullable = false)
    private boolean esDispositivoNuevo;

    @Column(name = "es_ip_nueva", nullable = false)
    private boolean esIpNueva;

    @Column(name = "es_pais_nuevo", nullable = false)
    private boolean esPaisNuevo;

    @Column(name = "es_ciudad_nueva", nullable = false)
    private boolean esCiudadNueva;

    @Column(name = "es_beneficiario_nuevo", nullable = false)
    private boolean esBeneficiarioNuevo;

    @Column(name = "es_horario_inusual", nullable = false)
    private boolean esHorarioInusual;

    @Column(name = "es_monto_inusual", nullable = false)
    private boolean esMontoInusual;

    @Column(name = "es_velocidad_inusual", nullable = false)
    private boolean esVelocidadInusual;

    @Column(name = "fecha_calculo", nullable = false)
    private OffsetDateTime fechaCalculo;
}

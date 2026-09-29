package com.lambdashield.fraude.entity;

import com.lambdashield.fraude.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "codigo_transaccion", nullable = false, unique = true)
    private UUID codigoTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_beneficiario")
    private Beneficiario beneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_transaccion", nullable = false)
    private TipoTransaccion tipoTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_canal", nullable = false)
    private Canal canal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dispositivo")
    private Dispositivo dispositivo;

    @Column(name = "monto_transaccion", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoTransaccion;

    @Column(name = "codigo_moneda", nullable = false, length = 3)
    private String codigoMoneda;

    @Column(name = "fecha_hora_transaccion", nullable = false)
    private OffsetDateTime fechaHoraTransaccion;

    @Column(name = "saldo_anterior", precision = 18, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", precision = 18, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "codigo_pais", length = 2)
    private String codigoPais;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transaccion", nullable = false, length = 20)
    private EstadoTransaccion estadoTransaccion;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @OneToMany(mappedBy = "transaccion")
    @ToString.Exclude
    private List<EvaluacionFraude> evaluaciones;

    @OneToOne(mappedBy = "transaccion", cascade = CascadeType.ALL)
    private CaracteristicaTransaccion caracteristica;
}

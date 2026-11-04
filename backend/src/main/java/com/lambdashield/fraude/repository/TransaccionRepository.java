package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.Transaccion;
import com.lambdashield.fraude.domain.enums.NaturalezaTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    Optional<Transaccion> findByCodigoTransaccion(UUID codigoTransaccion);
    boolean existsByCuenta_Cliente_CodigoClienteAndDireccionIp(String codigoCliente, String direccionIp);

    @Query("""
            select t.tipoTransaccion.naturaleza, coalesce(sum(t.montoTransaccion), 0)
            from Transaccion t
            where t.fechaHoraTransaccion >= :desde and t.fechaHoraTransaccion < :hasta
            group by t.tipoTransaccion.naturaleza
            """)
    List<Object[]> sumarMontosPorNaturaleza(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("""
            select t from Transaccion t
            join fetch t.cuenta c
            join fetch c.cliente
            join fetch t.tipoTransaccion
            where t.fechaHoraTransaccion >= :desde and t.fechaHoraTransaccion < :hasta
            order by t.fechaHoraTransaccion desc
            """)
    List<Transaccion> buscarDelPeriodo(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    List<Transaccion> findByFechaHoraTransaccionGreaterThanEqualAndFechaHoraTransaccionLessThanOrderByFechaHoraTransaccionDesc(
            OffsetDateTime desde, OffsetDateTime hasta);

    default BigDecimal sumarMonto(OffsetDateTime desde, OffsetDateTime hasta, NaturalezaTransaccion naturaleza) {
        return sumarMontosPorNaturaleza(desde, hasta).stream()
                .filter(fila -> fila[0] == naturaleza)
                .map(fila -> (BigDecimal) fila[1])
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}

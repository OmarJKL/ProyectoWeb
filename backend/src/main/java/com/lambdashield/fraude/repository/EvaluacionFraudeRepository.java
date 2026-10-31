package com.lambdashield.fraude.repository;

import com.lambdashield.fraude.domain.entity.EvaluacionFraude;
import com.lambdashield.fraude.domain.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EvaluacionFraudeRepository extends JpaRepository<EvaluacionFraude, Long> {
    Optional<EvaluacionFraude> findByTransaccion(Transaccion transaccion);
    List<EvaluacionFraude> findByEsActivaTrueOrderByFechaEvaluacionDesc();
    List<EvaluacionFraude> findByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteOrderByFechaEvaluacionDesc(
            String codigoCliente);
    Optional<EvaluacionFraude> findByIdEvaluacionAndEsActivaTrue(Long idEvaluacion);
    long countByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteAndFechaEvaluacionAfter(
            String codigoCliente, OffsetDateTime fecha);
}

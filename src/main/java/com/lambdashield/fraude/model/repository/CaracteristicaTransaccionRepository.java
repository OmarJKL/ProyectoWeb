package com.lambdashield.fraude.model.repository;

import com.lambdashield.fraude.model.entity.CaracteristicaTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaracteristicaTransaccionRepository extends JpaRepository<CaracteristicaTransaccion, Long> {
}
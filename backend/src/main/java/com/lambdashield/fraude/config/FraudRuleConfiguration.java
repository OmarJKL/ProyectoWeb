package com.lambdashield.fraude.config;

import com.lambdashield.fraude.domain.fraud.rule.ReglaBeneficiario;
import com.lambdashield.fraude.domain.fraud.rule.ReglaDispositivo;
import com.lambdashield.fraude.domain.fraud.rule.ReglaEvaluable;
import com.lambdashield.fraude.domain.fraud.rule.ReglaHorario;
import com.lambdashield.fraude.domain.fraud.rule.ReglaIp;
import com.lambdashield.fraude.domain.fraud.rule.ReglaMontoInusual;
import com.lambdashield.fraude.domain.fraud.rule.ReglaUbicacion;
import com.lambdashield.fraude.domain.fraud.rule.ReglaVelocidad;
import com.lambdashield.fraude.domain.fraud.rule.ReglaViajeImposible;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla las reglas de dominio con el contenedor de Spring. */
@Configuration
public class FraudRuleConfiguration {

    @Bean
    ReglaEvaluable reglaMontoInusual() {
        return new ReglaMontoInusual();
    }

    @Bean
    ReglaEvaluable reglaDispositivo() {
        return new ReglaDispositivo();
    }

    @Bean
    ReglaEvaluable reglaIp() {
        return new ReglaIp();
    }

    @Bean
    ReglaEvaluable reglaUbicacion() {
        return new ReglaUbicacion();
    }

    @Bean
    ReglaEvaluable reglaHorario() {
        return new ReglaHorario();
    }

    @Bean
    ReglaEvaluable reglaVelocidad() {
        return new ReglaVelocidad();
    }

    @Bean
    ReglaEvaluable reglaViajeImposible() {
        return new ReglaViajeImposible();
    }

    @Bean
    ReglaEvaluable reglaBeneficiario() {
        return new ReglaBeneficiario();
    }
}

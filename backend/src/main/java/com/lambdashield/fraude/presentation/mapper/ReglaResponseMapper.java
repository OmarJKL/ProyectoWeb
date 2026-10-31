package com.lambdashield.fraude.presentation.mapper;

import com.lambdashield.fraude.application.model.ReglaCatalogo;
import com.lambdashield.fraude.presentation.dto.response.ReglaResponse;
import org.springframework.stereotype.Component;

/** Traduce el catálogo de reglas al DTO HTTP. */
@Component
public class ReglaResponseMapper {

    public ReglaResponse toResponse(ReglaCatalogo regla) {
        return new ReglaResponse(regla.codigo(), regla.nombre(), regla.descripcion(), regla.pesoMaximo());
    }
}

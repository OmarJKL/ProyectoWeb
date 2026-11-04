package com.lambdashield.fraude.presentation.mapper;

import com.lambdashield.fraude.application.model.UsuarioResumen;
import com.lambdashield.fraude.presentation.dto.response.UsuarioResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Traduce el modelo de usuarios de la aplicación al DTO HTTP. */
@Component
public class UsuarioResponseMapper {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public UsuarioResponse toResponse(UsuarioResumen usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.usuario(),
                usuario.nombres(),
                usuario.correo(),
                usuario.estado(),
                usuario.rol(),
                usuario.codigoCliente(),
                usuario.ultimoAcceso() != null ? usuario.ultimoAcceso().format(FORMATO_FECHA) : "—"
        );
    }

    public List<UsuarioResponse> toResponses(List<UsuarioResumen> usuarios) {
        return usuarios.stream().map(this::toResponse).toList();
    }
}

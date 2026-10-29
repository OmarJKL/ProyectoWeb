package com.lambdashield.fraude.domain.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRolId implements Serializable {
    private Long usuario;
    private Short rol;
}

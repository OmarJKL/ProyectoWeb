package com.lambdashield.fraude.model.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRolId implements Serializable {
    private Long usuario;
    private Integer rol;
}

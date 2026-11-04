package com.lambdashield.fraude.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renderiza la página de gestión de usuarios (solo ADMIN). Los datos se
 * obtienen desde el navegador llamando a /api/v1/user.
 */
@Controller
public class UsuarioViewController {

    @GetMapping("/usuarios")
    public String usuarios() {
        return "usuarios";
    }
}

package com.lambdashield.fraude.controller.mvc;

import com.lambdashield.fraude.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String getLogin(HttpSession session) {
        if (session.getAttribute("usuario") != null) {
            String rol = (String) session.getAttribute("rol");
            if ("CLIENTE".equals(rol)) {
                return "redirect:/operacion";
            }
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(@RequestParam String usuario, @RequestParam String clave, HttpSession session, Model model) {
        if (authService.validar(usuario, clave)) {
            String rol = authService.obtenerRol(usuario);
            String nombre = authService.obtenerNombreUsuario(usuario);

            session.setAttribute("usuario", usuario);
            session.setAttribute("nombreUsuario", nombre);
            session.setAttribute("rol", rol);

            if ("CLIENTE".equals(rol)) {
                return "redirect:/operacion";
            }
            return "redirect:/";
        } else {
            model.addAttribute("error", true);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}

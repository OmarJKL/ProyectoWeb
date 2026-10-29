package com.lambdashield.fraude.presentation.web;

import com.lambdashield.fraude.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public String postLogin(@RequestParam String usuario, @RequestParam String clave,
                            HttpSession session, HttpServletRequest request, Model model) {
        var autenticado = authService.autenticar(usuario, clave);
        if (autenticado.isPresent()) {
            // Evita session fixation: se renueva el identificador de sesión tras autenticar.
            request.changeSessionId();

            AuthService.UsuarioSesion info = autenticado.get();
            session.setAttribute("usuario", info.usuario());
            session.setAttribute("nombreUsuario", info.nombreCompleto());
            session.setAttribute("rol", info.rol());
            session.setAttribute("codigoCliente", info.codigoCliente());

            if ("CLIENTE".equals(info.rol())) {
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

package com.lambdashield.fraude.controller.mvc;

import com.lambdashield.fraude.dto.response.ReglaResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReglaViewController {

    public static final List<ReglaResponse> REGLAS = List.of(
            new ReglaResponse("RF001", "Monto Inusual", "Condición: monto > 5 x promedio_30d. Penaliza operaciones que exceden drásticamente el consumo histórico del titular.", 25),
            new ReglaResponse("RF002", "Dispositivo Nuevo", "Condición: es_dispositivo_nuevo = true. Detecta accesos desde teléfonos o navegadores sin huella previa registrada.", 15),
            new ReglaResponse("RF003", "IP Nueva", "Condición: es_ip_nueva = true. Conexiones a la banca digital desde direcciones IP que no concuerdan con la red habitual.", 10),
            new ReglaResponse("RF004", "País Nuevo", "Condición: es_pais_nuevo = true. Penaliza transferencias u operaciones originadas en países o jurisdicciones no frecuentes.", 30),
            new ReglaResponse("RF005", "Horario Inusual", "Condición: es_horario_inusual = true. La franja de madrugada (00:00–05:00) concentra mayor riesgo probabilístico.", 10),
            new ReglaResponse("RF006", "Alta Frecuencia", "Condición: cantidad_transacciones_1h > 5. Ráfagas veloces de operaciones típicas de clonación o card testing.", 25),
            new ReglaResponse("RF007", "Viaje Imposible", "Condición: distancia / tiempo excede umbral físico. Detecta saltos geográficos incompatibles en intervalos cortos.", 40),
            new ReglaResponse("RF008", "Beneficiario Nuevo + Monto Alto", "Condición: nuevo && monto > umbral. Destinatario sin historial asociado a un importe elevado de fondos.", 20)
    );

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        model.addAttribute("reglas", REGLAS);
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        return "reglas";
    }
}

package com.lambdashield.fraude.application.service;

import com.lambdashield.fraude.application.model.ReglaCatalogo;
import com.lambdashield.fraude.domain.entity.ReglaFraude;
import com.lambdashield.fraude.repository.ReglaFraudeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReglaConsultaService {

    private final ReglaFraudeRepository reglaFraudeRepository;

    public ReglaConsultaService(ReglaFraudeRepository reglaFraudeRepository) {
        this.reglaFraudeRepository = reglaFraudeRepository;
    }

    @Transactional(readOnly = true)
    public List<ReglaCatalogo> listar() {
        return reglaFraudeRepository.findByEsActivaTrueOrderByCodigoReglaAsc()
                .stream()
                .map(this::toCatalogo)
                .toList();
    }

    private ReglaCatalogo toCatalogo(ReglaFraude regla) {
        int peso = regla.getPeso() != null ? regla.getPeso().intValue() : 0;
        return new ReglaCatalogo(regla.getCodigoRegla(), regla.getNombreRegla(), regla.getDescripcion(), peso);
    }
}

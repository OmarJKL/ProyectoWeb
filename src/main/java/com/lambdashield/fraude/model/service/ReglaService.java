package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.entity.ReglaFraude;
import com.lambdashield.fraude.model.repository.ReglaFraudeRepository;
import com.lambdashield.fraude.model.response.ReglaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReglaService {

    private final ReglaFraudeRepository reglaFraudeRepository;

    public ReglaService(ReglaFraudeRepository reglaFraudeRepository) {
        this.reglaFraudeRepository = reglaFraudeRepository;
    }

    @Transactional(readOnly = true)
    public List<ReglaResponse> listar() {
        return reglaFraudeRepository.findByEsActivaTrueOrderByCodigoReglaAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReglaResponse toResponse(ReglaFraude regla) {
        int peso = regla.getPeso() != null ? regla.getPeso().intValue() : 0;
        return new ReglaResponse(
                regla.getCodigoRegla(),
                regla.getNombreRegla(),
                regla.getDescripcion(),
                peso
        );
    }
}

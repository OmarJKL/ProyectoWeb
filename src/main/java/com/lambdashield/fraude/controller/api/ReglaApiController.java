package com.lambdashield.fraude.controller.api;

import com.lambdashield.fraude.controller.mvc.ReglaViewController;
import com.lambdashield.fraude.dto.response.ReglaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reglas")
public class ReglaApiController {

    @GetMapping
    public ResponseEntity<List<ReglaResponse>> listar() {
        return ResponseEntity.ok(ReglaViewController.REGLAS);
    }
}

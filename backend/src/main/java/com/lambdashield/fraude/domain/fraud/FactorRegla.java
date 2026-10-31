package com.lambdashield.fraude.domain.fraud;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Identidad estable entre los factores de scoring y los códigos de catálogo RF. */
public enum FactorRegla {
    MONTO("monto", "RF001"),
    DISPOSITIVO("dispositivo", "RF002"),
    IP("ip", "RF003"),
    UBICACION("ubicacion", "RF004"),
    HORARIO("horario", "RF005"),
    VELOCIDAD("velocidad", "RF006"),
    VIAJE_IMPOSIBLE("viaje_imposible", "RF007"),
    BENEFICIARIO("beneficiario", "RF008");

    private static final Map<String, FactorRegla> POR_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(FactorRegla::id, Function.identity()));
    private static final Map<String, FactorRegla> POR_CODIGO = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(FactorRegla::codigo, Function.identity()));

    private final String id;
    private final String codigo;

    FactorRegla(String id, String codigo) {
        this.id = id;
        this.codigo = codigo;
    }

    public String id() {
        return id;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<FactorRegla> porId(String id) {
        return Optional.ofNullable(POR_ID.get(id));
    }

    public static Optional<FactorRegla> porCodigo(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }
}

package pe.sga.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CategoriaLicenciaMtc {
    A_IIB("A-IIb"),
    A_IIIB("A-IIIb"),
    A_IIIC("A-IIIc"),
    A_IV("A-IV");

    private final String codigo;

    CategoriaLicenciaMtc(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator
    public static CategoriaLicenciaMtc fromCodigo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (CategoriaLicenciaMtc cat : values()) {
            if (cat.codigo.equalsIgnoreCase(trimmed) || cat.name().equalsIgnoreCase(trimmed)) {
                return cat;
            }
        }
        throw new IllegalArgumentException("Categoría de licencia MTC no válida: " + value);
    }
}

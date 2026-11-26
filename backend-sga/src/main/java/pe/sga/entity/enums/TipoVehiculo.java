package pe.sga.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoVehiculo {
    TRACTO,
    PLATAFORMA,
    FURGON,
    CAMION_BARANDA;

    @JsonCreator
    public static TipoVehiculo fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("TRACTO_PLATAFORMA".equals(normalized)) {
            return TRACTO;
        }
        return TipoVehiculo.valueOf(normalized);
    }
}

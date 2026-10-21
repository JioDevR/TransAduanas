package pe.sga.entity.enums;

import lombok.Getter;

@Getter
public enum RolUsuario {
    ADMINISTRADOR_TI("Administrador de TI"),
    GERENTE_GENERAL("Gerente General"),
    AGENTE_COMERCIAL("Agente Comercial"),
    SUPERVISOR_ADUANERO("Supervisor Aduanero"),
    AGENTE_ADUANERO("Agente Aduanero"),
    SUPERVISOR_TRANSPORTE("Supervisor de Transporte"),
    CONTADOR("Contador"),
    ASESOR_LEGAL("Asesor Legal");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}

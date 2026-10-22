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
    ASESOR_LEGAL("Asesor Legal"),

    // Aliases para compatibilidad hacia atrás con registros históricos en base de datos
    ADMIN_TI("Administrador de TI"),
    ADMINISTRADOR("Administrador de TI"),
    GERENTE("Gerente General"),
    VENTAS("Agente Comercial"),
    SUPERVISION("Supervisor Aduanero"),
    LIQUIDADOR("Supervisor Aduanero"),
    TRANSPORTE("Supervisor de Transporte"),
    LOGISTICA("Supervisor de Transporte"),
    FINANZAS("Contador"),
    LEGAL("Asesor Legal");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Resuelve el enum normativo estándar correspondiente para auditoría y RBAC.
     */
    public RolUsuario toStandard() {
        return switch (this) {
            case ADMIN_TI, ADMINISTRADOR -> ADMINISTRADOR_TI;
            case GERENTE -> GERENTE_GENERAL;
            case VENTAS -> AGENTE_COMERCIAL;
            case SUPERVISION, LIQUIDADOR -> SUPERVISOR_ADUANERO;
            case TRANSPORTE, LOGISTICA -> SUPERVISOR_TRANSPORTE;
            case FINANZAS -> CONTADOR;
            case LEGAL -> ASESOR_LEGAL;
            default -> this;
        };
    }
}

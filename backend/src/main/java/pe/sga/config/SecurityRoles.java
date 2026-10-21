package pe.sga.config;

public final class SecurityRoles {

    private SecurityRoles() {
        // Clase de constantes, no instanciable
    }

    // Códigos de roles definitivos (sin prefijo ROLE_ para hasRole / hasAnyRole)
    public static final String ADMINISTRADOR_TI = "ADMINISTRADOR_TI";
    public static final String GERENTE_GENERAL = "GERENTE_GENERAL";
    public static final String AGENTE_COMERCIAL = "AGENTE_COMERCIAL";
    public static final String SUPERVISOR_ADUANERO = "SUPERVISOR_ADUANERO";
    public static final String AGENTE_ADUANERO = "AGENTE_ADUANERO";
    public static final String SUPERVISOR_TRANSPORTE = "SUPERVISOR_TRANSPORTE";
    public static final String CONTADOR = "CONTADOR";
    public static final String ASESOR_LEGAL = "ASESOR_LEGAL";

    // Expresiones SpEL reutilizables para @PreAuthorize
    public static final String HAS_ROLE_ADMIN_TI = "hasRole('" + ADMINISTRADOR_TI + "')";
    public static final String HAS_ROLE_GERENTE_GENERAL = "hasRole('" + GERENTE_GENERAL + "')";
    public static final String HAS_ANY_ADMIN = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL + "')";

    // Matriz de accesos por módulo/dominio:

    // 1. Usuarios, Roles y Logs
    public static final String ACCESS_USUARIOS_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL
            + "')";
    public static final String ACCESS_USUARIOS_WRITE = "hasRole('" + ADMINISTRADOR_TI + "')";
    public static final String ACCESS_ROLES_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL + "')";
    public static final String ACCESS_ROLES_WRITE = "hasRole('" + ADMINISTRADOR_TI + "')";
    public static final String ACCESS_LOGS_READ = "hasRole('" + ADMINISTRADOR_TI + "')";

    // 2. Sedes y Catálogos auxiliares (Puerto, Tipo Carga, Mercancía)
    public static final String ACCESS_SEDES_CATALOGOS_READ = "isAuthenticated()";
    public static final String ACCESS_SEDES_CATALOGOS_WRITE = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "')";

    // 3. Clientes y Cotizaciones
    public static final String ACCESS_CLIENTE_COTIZACION_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "', '" + AGENTE_COMERCIAL + "', '" + SUPERVISOR_ADUANERO + "', '" + AGENTE_ADUANERO
            + "', '" + CONTADOR + "', '" + ASESOR_LEGAL + "')";
    public static final String ACCESS_CLIENTE_COTIZACION_WRITE = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "', '" + AGENTE_COMERCIAL + "')";
    public static final String ACCESS_COTIZACION_APROBACION = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "', '" + SUPERVISOR_ADUANERO + "', '" + AGENTE_ADUANERO + "')";

    // 4. Expedientes y Control de Almacén
    public static final String ACCESS_EXPEDIENTE_ALMACEN_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "', '" + SUPERVISOR_ADUANERO + "', '" + AGENTE_ADUANERO + "', '" + ASESOR_LEGAL + "', '"
            + SUPERVISOR_TRANSPORTE + "', '" + CONTADOR + "')";
    public static final String ACCESS_EXPEDIENTE_ALMACEN_WRITE = "hasAnyRole('" + ADMINISTRADOR_TI + "', '"
            + GERENTE_GENERAL + "', '" + SUPERVISOR_ADUANERO + "', '" + AGENTE_ADUANERO + "')";

    // 5. Despacho y Transporte (Conductores, Unidades, Despachos)
    public static final String ACCESS_TRANSPORTE_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL
            + "', '" + SUPERVISOR_TRANSPORTE + "', '" + SUPERVISOR_ADUANERO + "', '" + AGENTE_ADUANERO + "')";
    public static final String ACCESS_TRANSPORTE_WRITE = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL
            + "', '" + SUPERVISOR_TRANSPORTE + "')";

    // 6. Liquidación Financiera
    public static final String ACCESS_LIQUIDACION_READ = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL
            + "', '" + CONTADOR + "', '" + SUPERVISOR_ADUANERO + "', '" + ASESOR_LEGAL + "')";
    public static final String ACCESS_LIQUIDACION_WRITE = "hasAnyRole('" + ADMINISTRADOR_TI + "', '" + GERENTE_GENERAL
            + "', '" + CONTADOR + "', '" + SUPERVISOR_ADUANERO + "')";

    // 7. Documentos (Adjuntos a cotizaciones / expedientes)
    public static final String ACCESS_DOCUMENTOS_READ = "isAuthenticated()";
    public static final String ACCESS_DOCUMENTOS_WRITE = "isAuthenticated()";
}

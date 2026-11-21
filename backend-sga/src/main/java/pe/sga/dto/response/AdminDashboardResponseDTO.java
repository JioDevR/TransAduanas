package pe.sga.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponseDTO {
    private AdminSistemaEstadoDTO sistema;

    // Usuarios internos
    private long usuariosTotal;
    private long usuariosActivos;
    private long usuariosInactivos;
    private Map<String, Long> distribucionRoles;

    // Usuarios Portal B2B
    private long portalUsuariosTotal;
    private long portalUsuariosActivos;
    private long portalUsuariosInactivos;
    private long portalCambioPendiente;
    private long portalEmpresasVinculadas;

    // Auditoría del período
    private int diasPeriodo;
    private long auditoriaTotalPeriodo;
    private long auditoriaExitosPeriodo;
    private long auditoriaFallosPeriodo;
    private long loginsFallidosPeriodo;
    private List<SerieDiariaDTO> actividadDiaria;
    private List<AuditoriaEventoDTO> eventosRecientes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SerieDiariaDTO {
        private String fecha;
        private long exitos;
        private long fallos;
    }
}

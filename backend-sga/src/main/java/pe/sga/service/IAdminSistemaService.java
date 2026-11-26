package pe.sga.service;

import pe.sga.dto.response.AdminAlmacenamientoDTO;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.RolMatrizDTO;

import java.util.List;
import java.util.Map;

public interface IAdminSistemaService {

    AdminSistemaEstadoDTO obtenerEstadoSistema();

    AdminDashboardResponseDTO obtenerDashboard(int dias, String moduloFiltro);

    List<RolMatrizDTO> obtenerMatrizRoles();

    AdminAlmacenamientoDTO obtenerDiagnosticoAlmacenamiento(
            String buscar, String entidad, String categoria, String integridad, int page, int size);

    Map<String, Object> depurarArchivosHuerfanos();
}

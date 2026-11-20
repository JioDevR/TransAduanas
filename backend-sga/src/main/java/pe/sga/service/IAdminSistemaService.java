package pe.sga.service;

import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.RolMatrizDTO;

import java.util.List;

public interface IAdminSistemaService {

    AdminSistemaEstadoDTO obtenerEstadoSistema();

    AdminDashboardResponseDTO obtenerDashboard(int dias, String moduloFiltro);

    List<RolMatrizDTO> obtenerMatrizRoles();
}

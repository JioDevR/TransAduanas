package pe.sga.service;

import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AuditoriaEventoDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IAuditoriaService {

    void registrar(String modulo, String accion, String entidad, String idEntidad, String detalle, String resultado);

    void registrarConActor(String usuarioActor, String rolActor, String modulo, String accion,
                           String entidad, String idEntidad, String detalle, String resultado);

    Map<String, Object> listar(String buscar, String modulo, String resultado,
                               LocalDateTime desde, LocalDateTime hasta, int page, int size);

    List<AuditoriaEventoDTO> listarRecientes(int limit);

    List<AdminDashboardResponseDTO.SerieDiariaDTO> obtenerSerieDiaria(int dias, String moduloFiltro);
}

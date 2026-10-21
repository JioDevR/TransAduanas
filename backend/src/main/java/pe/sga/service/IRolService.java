package pe.sga.service;

import pe.sga.dto.request.RolDTO;
import pe.sga.dto.response.RolResponseDTO;

import java.util.List;

public interface IRolService {

    List<RolResponseDTO> listarTodos();

    List<RolResponseDTO> listarActivos();

    RolResponseDTO buscarPorId(Integer idRol);

    RolResponseDTO crear(RolDTO dto);

    RolResponseDTO actualizar(Integer idRol, RolDTO dto);

    void eliminarPorEstado(Integer idRol);

    void reactivar(Integer idRol);
}

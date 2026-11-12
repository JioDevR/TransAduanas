package pe.sga.service;

import pe.sga.dto.response.RolResponseDTO;

import java.util.List;

public interface IRolService {

    List<RolResponseDTO> listarTodos();

    List<RolResponseDTO> listarActivos();

    RolResponseDTO buscarPorId(Integer idRol);
}

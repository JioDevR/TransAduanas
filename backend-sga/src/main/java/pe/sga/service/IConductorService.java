package pe.sga.service;

import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;

import java.util.List;

public interface IConductorService {

    List<ConductorResponseDTO> listarTodos();

    List<ConductorResponseDTO> listarActivos();

    ConductorResponseDTO buscarPorId(Integer idConductor);

    ConductorResponseDTO buscarPorLicencia(String numeroLicencia);

    ConductorResponseDTO crear(ConductorRequestDTO dto);

    ConductorResponseDTO actualizar(Integer idConductor, ConductorRequestDTO dto);

    void eliminar(Integer idConductor);

    void reactivar(Integer idConductor);
}

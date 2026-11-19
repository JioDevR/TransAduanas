package pe.sga.service;

import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;

import java.util.List;

public interface IUnidadTransporteService {

    List<UnidadTransporteResponseDTO> listarTodas();

    List<UnidadTransporteResponseDTO> listarActivas();

    List<UnidadTransporteResponseDTO> listarDisponibles();

    UnidadTransporteResponseDTO buscarPorId(Integer idUnidad);

    UnidadTransporteResponseDTO buscarPorPlaca(String placaTracto);

    UnidadTransporteResponseDTO crear(UnidadTransporteRequestDTO dto);

    UnidadTransporteResponseDTO actualizar(Integer idUnidad, UnidadTransporteRequestDTO dto);

    void eliminar(Integer idUnidad);

    void reactivar(Integer idUnidad);
}

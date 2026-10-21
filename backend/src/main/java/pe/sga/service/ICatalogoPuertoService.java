package pe.sga.service;

import pe.sga.dto.request.CatalogoPuertoDTO;
import pe.sga.dto.response.CatalogoPuertoResponseDTO;

import java.util.List;

public interface ICatalogoPuertoService {

    List<CatalogoPuertoResponseDTO> listarTodos();

    List<CatalogoPuertoResponseDTO> listarActivos();

    CatalogoPuertoResponseDTO buscarPorId(Integer idPuerto);

    CatalogoPuertoResponseDTO crear(CatalogoPuertoDTO dto);

    CatalogoPuertoResponseDTO actualizar(Integer idPuerto, CatalogoPuertoDTO dto);

    void eliminarPorEstado(Integer idPuerto);

    void reactivar(Integer idPuerto);
}

package pe.sga.service;

import pe.sga.dto.request.CatalogoMercanciaRequestDTO;
import pe.sga.dto.response.CatalogoMercanciaResponseDTO;

import java.util.List;

public interface ICatalogoMercanciaService {

    List<CatalogoMercanciaResponseDTO> listarTodos();

    List<CatalogoMercanciaResponseDTO> listarActivos();

    List<CatalogoMercanciaResponseDTO> listarPorCategoria(String categoria);

    CatalogoMercanciaResponseDTO buscarPorId(Integer idMercancia);

    CatalogoMercanciaResponseDTO buscarPorCodigo(String codigo);

    CatalogoMercanciaResponseDTO crear(CatalogoMercanciaRequestDTO dto);

    CatalogoMercanciaResponseDTO actualizar(Integer idMercancia, CatalogoMercanciaRequestDTO dto);

    void eliminarPorEstado(Integer idMercancia);

    void reactivar(Integer idMercancia);
}

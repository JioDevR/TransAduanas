package pe.sga.service;

import pe.sga.dto.request.CatalogoTipoCargaDTO;
import pe.sga.dto.response.CatalogoTipoCargaResponseDTO;

import java.util.List;

public interface ICatalogoTipoCargaService {

    List<CatalogoTipoCargaResponseDTO> listarTodos();

    List<CatalogoTipoCargaResponseDTO> listarActivos();

    CatalogoTipoCargaResponseDTO buscarPorId(Integer idTipoCarga);

    CatalogoTipoCargaResponseDTO crear(CatalogoTipoCargaDTO dto);

    CatalogoTipoCargaResponseDTO actualizar(Integer idTipoCarga, CatalogoTipoCargaDTO dto);

    void eliminarPorEstado(Integer idTipoCarga);

    void reactivar(Integer idTipoCarga);
}

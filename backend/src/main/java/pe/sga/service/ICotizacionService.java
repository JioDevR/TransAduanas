package pe.sga.service;

import pe.sga.dto.request.CotizacionRequestDTO;
import pe.sga.dto.request.CotizacionRevisionDTO;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.enums.EstadoCotizacion;

import java.util.List;

public interface ICotizacionService {

    List<CotizacionResponseDTO> listarTodas(Boolean soloActivas);

    List<CotizacionResponseDTO> listarPorEstado(EstadoCotizacion estado);

    List<CotizacionResponseDTO> listarPorCliente(Integer idCliente);

    CotizacionResponseDTO buscarPorId(Integer id);

    CotizacionResponseDTO buscarPorCodigo(String codigoCotizacion);

    CotizacionResponseDTO crear(CotizacionRequestDTO dto);

    CotizacionResponseDTO actualizar(Integer id, CotizacionRequestDTO dto);

    CotizacionResponseDTO revisarTecnicamente(Integer id, CotizacionRevisionDTO dto);

    CotizacionResponseDTO cambiarEstado(Integer id, EstadoCotizacion estado, String motivoObservacion);

    void anular(Integer id);

    void eliminar(Integer id);
}

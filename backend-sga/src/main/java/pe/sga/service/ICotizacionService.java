package pe.sga.service;

import pe.sga.dto.request.CotizacionRequestDTO;
import pe.sga.dto.request.CotizacionRevisionDTO;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;

import java.util.List;

public interface ICotizacionService {

    List<CotizacionResponseDTO> listar(
            Boolean soloActivas,
            EstadoCotizacion estado,
            Integer idCliente,
            Integer idUsuarioComercial,
            Usuario usuarioActual
    );

    List<CotizacionResponseDTO> listarSinAsignar();

    CotizacionResponseDTO buscarPorId(Integer id, Usuario usuarioActual);

    CotizacionResponseDTO buscarPorCodigo(String codigoCotizacion, Usuario usuarioActual);

    CotizacionResponseDTO crear(CotizacionRequestDTO dto, Usuario usuarioActual);

    CotizacionResponseDTO actualizar(Integer id, CotizacionRequestDTO dto, Usuario usuarioActual);

    CotizacionResponseDTO asignarAsesor(Integer id, Integer idUsuarioComercial, Usuario usuarioActual);

    CotizacionResponseDTO revisarTecnicamente(Integer id, CotizacionRevisionDTO dto);

    CotizacionResponseDTO cambiarEstado(Integer id, EstadoCotizacion estado, String motivoObservacion);

    void anular(Integer id, Usuario usuarioActual);

    void eliminar(Integer id, Usuario usuarioActual);
}

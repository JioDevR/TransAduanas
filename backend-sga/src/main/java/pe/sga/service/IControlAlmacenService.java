package pe.sga.service;

import pe.sga.dto.request.ControlAlmacenRequestDTO;
import pe.sga.dto.request.ControlAlmacenSalidaDTO;
import pe.sga.dto.response.ControlAlmacenResponseDTO;

import java.util.List;

public interface IControlAlmacenService {

    List<ControlAlmacenResponseDTO> listarTodos(Boolean soloActivos);

    List<ControlAlmacenResponseDTO> listarPorDeposito(String depositoTemporal);

    ControlAlmacenResponseDTO buscarPorId(Integer idControlAlmacen);

    ControlAlmacenResponseDTO buscarPorExpediente(Integer idExpediente);

    ControlAlmacenResponseDTO registrarIngreso(ControlAlmacenRequestDTO dto);

    ControlAlmacenResponseDTO actualizar(Integer idControlAlmacen, ControlAlmacenRequestDTO dto);

    ControlAlmacenResponseDTO autorizarSalida(Integer idControlAlmacen, ControlAlmacenSalidaDTO dto);

    void anular(Integer idControlAlmacen);
}

package pe.sga.service;

import pe.sga.dto.request.ClienteRequestDTO;
import pe.sga.dto.response.ClienteResponseDTO;

import java.util.List;

public interface IClienteService {

    List<ClienteResponseDTO> listarTodos();

    List<ClienteResponseDTO> listarActivos();

    List<ClienteResponseDTO> listarPorTipoProspecto(Boolean esProspecto, Boolean soloActivos);

    List<ClienteResponseDTO> buscarPorTermino(String termino, Boolean soloActivos);

    ClienteResponseDTO buscarPorId(Integer id);

    ClienteResponseDTO buscarPorNumeroDocumento(String numeroDocumento);

    ClienteResponseDTO crear(ClienteRequestDTO dto);

    ClienteResponseDTO actualizar(Integer id, ClienteRequestDTO dto);

    void eliminar(Integer id);

    void reactivar(Integer id);

    ClienteResponseDTO convertirEnCliente(Integer id);
}

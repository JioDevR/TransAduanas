package pe.sga.service;

import pe.sga.dto.portal.admin.*;

import java.util.List;

public interface IUsuarioClienteService {

    List<UsuarioClienteResponseDTO> listar(Integer idCliente, Boolean soloActivos, String buscar);

    default List<UsuarioClienteResponseDTO> listar(Integer idCliente, Boolean soloActivos, String rolPortal, String buscar) {
        return listar(idCliente, soloActivos, buscar);
    }

    UsuarioClienteResponseDTO buscarPorId(Integer id);

    UsuarioClienteResponseDTO crear(UsuarioClienteCreateDTO dto);

    UsuarioClienteResponseDTO actualizar(Integer id, UsuarioClienteUpdateDTO dto);

    void eliminarPorEstado(Integer id);

    void reactivar(Integer id);

    void resetearPassword(Integer id, UsuarioClienteResetPasswordDTO dto);
}

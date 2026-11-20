package pe.sga.service;

import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.dto.request.UsuarioCreateDTO;
import pe.sga.dto.request.UsuarioUpdateDTO;
import pe.sga.dto.response.AuthResponseDTO;
import pe.sga.dto.response.UsuarioResponseDTO;

import java.util.List;

public interface IUsuarioService {

    AuthResponseDTO autenticar(LoginRequestDTO loginRequest);

    List<UsuarioResponseDTO> listarTodos();

    List<UsuarioResponseDTO> listarActivos();

    UsuarioResponseDTO buscarPorId(Integer idUsuario);

    UsuarioResponseDTO buscarPorUsername(String username);

    UsuarioResponseDTO crear(UsuarioCreateDTO dto);

    UsuarioResponseDTO actualizar(Integer idUsuario, UsuarioUpdateDTO dto);

    void eliminarPorEstado(Integer idUsuario);

    void reactivar(Integer idUsuario);
}

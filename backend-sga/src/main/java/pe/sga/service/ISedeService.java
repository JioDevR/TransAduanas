package pe.sga.service;

import pe.sga.dto.request.SedeCreateDTO;
import pe.sga.dto.request.SedeUpdateDTO;
import pe.sga.dto.response.SedeResponseDTO;

import java.util.List;

public interface ISedeService {

    List<SedeResponseDTO> listarTodas();

    List<SedeResponseDTO> listarOperativas();

    SedeResponseDTO buscarPorId(Integer idSede);

    SedeResponseDTO crear(SedeCreateDTO dto);

    SedeResponseDTO actualizar(Integer idSede, SedeUpdateDTO dto);

    void eliminarPorEstado(Integer idSede);

    void reactivar(Integer idSede);
}

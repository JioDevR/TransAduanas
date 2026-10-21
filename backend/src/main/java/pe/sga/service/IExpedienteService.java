package pe.sga.service;

import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.util.List;

public interface IExpedienteService {

    List<ExpedienteResponseDTO> listarTodos(Boolean soloActivos);

    List<ExpedienteResponseDTO> listarPorCanal(CanalSunat canal);

    List<ExpedienteResponseDTO> listarPorEstado(EstadoExpediente estado);

    List<ExpedienteResponseDTO> listarPorAgente(Integer idAgenteAduanero);

    List<ExpedienteResponseDTO> listarPorCliente(Integer idCliente);

    ExpedienteResponseDTO buscarPorId(Integer idExpediente);

    ExpedienteResponseDTO buscarPorCodigo(String codigoExpediente);

    ExpedienteResponseDTO buscarPorCotizacion(Integer idCotizacion);

    ExpedienteResponseDTO crear(ExpedienteCrearDTO dto);

    ExpedienteResponseDTO actualizar(Integer idExpediente, ExpedienteActualizarDTO dto);

    ExpedienteResponseDTO asignarCanal(Integer idExpediente, ExpedienteCanalDTO dto);

    ExpedienteResponseDTO autorizarLevante(Integer idExpediente, ExpedienteLevanteDTO dto);

    void anular(Integer idExpediente);
}

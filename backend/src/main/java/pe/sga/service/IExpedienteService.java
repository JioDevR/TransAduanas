package pe.sga.service;

import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCerrarDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteDiligenciaDTO;
import pe.sga.dto.request.ExpedienteEmbarqueExportacionDTO;
import pe.sga.dto.request.ExpedienteIngresoOrigenDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.request.ExpedienteRecepcionExteriorDTO;
import pe.sga.dto.request.ExpedienteRecojoExportacionDTO;
import pe.sga.dto.request.ExpedienteRetiroDirectoDTO;
import pe.sga.dto.request.ExpedienteSolicitudTransporteDTO;
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

    ExpedienteResponseDTO solicitarRecojoExportacion(Integer idExpediente, ExpedienteRecojoExportacionDTO dto);

    ExpedienteResponseDTO confirmarIngresoOrigen(Integer idExpediente, ExpedienteIngresoOrigenDTO dto);

    ExpedienteResponseDTO asignarCanal(Integer idExpediente, ExpedienteCanalDTO dto);

    ExpedienteResponseDTO registrarDiligencia(Integer idExpediente, ExpedienteDiligenciaDTO dto);

    ExpedienteResponseDTO autorizarLevante(Integer idExpediente, ExpedienteLevanteDTO dto);

    ExpedienteResponseDTO confirmarEmbarqueOrigen(Integer idExpediente, ExpedienteEmbarqueExportacionDTO dto);

    ExpedienteResponseDTO confirmarRecepcionExterior(Integer idExpediente, ExpedienteRecepcionExteriorDTO dto);

    ExpedienteResponseDTO solicitarTransporte(Integer idExpediente, ExpedienteSolicitudTransporteDTO dto);

    ExpedienteResponseDTO registrarRetiroDirecto(Integer idExpediente, ExpedienteRetiroDirectoDTO dto);

    ExpedienteResponseDTO cerrar(Integer idExpediente, ExpedienteCerrarDTO dto);

    void anular(Integer idExpediente);
}



package pe.sga.service;

import pe.sga.dto.request.DespachoAsignarRecursosDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoRutaDTO;
import pe.sga.dto.request.DespachoTransporteCrearDTO;
import pe.sga.dto.response.DespachoTransporteResponseDTO;

import java.util.List;

public interface IDespachoTransporteService {

    List<DespachoTransporteResponseDTO> listarConFiltros(
            boolean soloActivos,
            String estado,
            Integer idConductor,
            Integer idUnidad);

    DespachoTransporteResponseDTO buscarPorId(Integer idDespachoTransporte);

    DespachoTransporteResponseDTO buscarPorTracking(String codigoTracking);

    DespachoTransporteResponseDTO buscarPorExpediente(Integer idExpediente);

    DespachoTransporteResponseDTO programar(DespachoTransporteCrearDTO dto);

    DespachoTransporteResponseDTO aceptarSolicitud(Integer idDespachoTransporte);

    DespachoTransporteResponseDTO asignarRecursos(Integer idDespachoTransporte, DespachoAsignarRecursosDTO dto);

    DespachoTransporteResponseDTO iniciarRuta(Integer idDespachoTransporte, DespachoRutaDTO dto);

    DespachoTransporteResponseDTO confirmarEntrega(Integer idDespachoTransporte, DespachoEntregaDTO dto);

    DespachoTransporteResponseDTO reportarIncidencia(Integer idDespachoTransporte, String incidencia);

    void anular(Integer idDespachoTransporte);
}

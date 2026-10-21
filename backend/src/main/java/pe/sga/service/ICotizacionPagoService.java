package pe.sga.service;

import pe.sga.dto.request.CotizacionPagoRegistrarDTO;
import pe.sga.dto.request.CotizacionPagoValidarDTO;
import pe.sga.dto.response.CotizacionPagoResponseDTO;
import pe.sga.entity.enums.EstadoPagoCotizacion;

import java.util.List;

public interface ICotizacionPagoService {

    CotizacionPagoResponseDTO registrarPago(CotizacionPagoRegistrarDTO dto, Integer idUsuarioRegistro);

    List<CotizacionPagoResponseDTO> listarPorCotizacion(Integer idCotizacion);

    List<CotizacionPagoResponseDTO> listarPorEstado(EstadoPagoCotizacion estadoPago);

    List<CotizacionPagoResponseDTO> listarTodos();

    CotizacionPagoResponseDTO validarPago(Integer idPago, CotizacionPagoValidarDTO dto, Integer idUsuarioFinanzas);

    CotizacionPagoResponseDTO buscarPorId(Integer idPago);
}

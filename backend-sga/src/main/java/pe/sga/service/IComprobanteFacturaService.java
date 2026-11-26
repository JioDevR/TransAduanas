package pe.sga.service;

import pe.sga.dto.response.ComprobanteFacturaResponseDTO;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.LiquidacionFinanciera;
import pe.sga.entity.Usuario;

import java.util.List;

public interface IComprobanteFacturaService {

    List<ComprobanteFacturaResponseDTO> listarTodas();

    List<ComprobanteFacturaResponseDTO> listarPorCliente(Integer idCliente);

    ComprobanteFacturaResponseDTO buscarPorId(Integer idComprobante);

    ComprobanteFacturaResponseDTO buscarPorIdParaCliente(Integer idCliente, Integer idComprobante);

    ComprobanteFacturaResponseDTO emitirPorAbono(Integer idPago, Integer idUsuarioEmisor);

    ComprobanteFacturaResponseDTO emitirAutomaticoPorAbono(CotizacionPago pago, Usuario usuarioEmisor);

    ComprobanteFacturaResponseDTO emitirPorLiquidacion(Integer idLiquidacion, Integer idUsuarioEmisor);

    ComprobanteFacturaResponseDTO emitirAutomaticoPorLiquidacion(LiquidacionFinanciera liquidacion, Usuario usuarioEmisor);

    ComprobanteFacturaResponseDTO anular(Integer idComprobante, Integer idUsuario);
}

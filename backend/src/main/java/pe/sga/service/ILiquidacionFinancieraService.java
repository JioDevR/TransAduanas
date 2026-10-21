package pe.sga.service;

import pe.sga.dto.request.LiquidacionFinancieraRequestDTO;
import pe.sga.dto.request.LiquidacionPagoDTO;
import pe.sga.dto.response.FinanzasMetricasResponseDTO;
import pe.sga.dto.response.LiquidacionEstimadoResponseDTO;
import pe.sga.dto.response.LiquidacionFinancieraResponseDTO;

import java.util.List;

public interface ILiquidacionFinancieraService {

    List<LiquidacionFinancieraResponseDTO> listarTodas(Boolean soloPendientes);

    List<LiquidacionFinancieraResponseDTO> listarPorEstado(String estadoPago);

    LiquidacionFinancieraResponseDTO buscarPorId(Integer idLiquidacion);

    LiquidacionFinancieraResponseDTO buscarPorCodigo(String codigoLiquidacion);

    LiquidacionFinancieraResponseDTO buscarPorExpediente(Integer idExpediente);

    LiquidacionEstimadoResponseDTO calcularEstimado(Integer idExpediente);

    LiquidacionFinancieraResponseDTO emitir(LiquidacionFinancieraRequestDTO dto, Integer idUsuarioAutenticado);

    LiquidacionFinancieraResponseDTO emitir(LiquidacionFinancieraRequestDTO dto);

    LiquidacionFinancieraResponseDTO actualizar(Integer idLiquidacion, LiquidacionFinancieraRequestDTO dto);

    LiquidacionFinancieraResponseDTO registrarPago(Integer idLiquidacion, LiquidacionPagoDTO dto);

    void anular(Integer idLiquidacion);

    FinanzasMetricasResponseDTO obtenerMetricas();
}

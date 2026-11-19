package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionFinancieraResponseDTO {

    private Integer idLiquidacion;
    private String codigoLiquidacion;

    // Expediente asociado
    private Integer idExpediente;
    private String codigoExpediente;
    private String numeroDam;
    private String canalSunat;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;

    // Usuario Liquidador
    private Integer idUsuarioLiquidador;
    private String nombreLiquidador;

    // Desglose de Rubros Base
    private BigDecimal totalTributosSunat;
    private BigDecimal gastosAlmacenPuerto;
    private BigDecimal fleteTransporteLocal;
    private BigDecimal honorariosAgencia;

    // Cálculo Impositivo y Detracción SPOT
    private BigDecimal subtotalImponible;
    private BigDecimal igvMonto;
    private Boolean aplicaDetraccion;
    private BigDecimal porcentajeDetraccion;
    private BigDecimal montoDetraccion;

    // Totales y Tipo de Cambio
    private BigDecimal totalGeneralPen;
    private BigDecimal totalGeneralUsd;
    private BigDecimal tipoCambio;

    // Estado y Pagos
    private String estadoPago;
    private String numeroFactura;
    private Integer idDocumentoFactura;
    private String nombreDocumentoFactura;

    private String metodoPago;
    private String bancoDestino;
    private String numeroOperacion;
    private Integer idDocumentoVoucher;
    private String nombreDocumentoVoucher;

    private LocalDate fechaEmision;
    private LocalDateTime fechaPago;
    private String observaciones;
}

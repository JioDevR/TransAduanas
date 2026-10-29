package pe.sga.dto.portal;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalLiquidacionDTO {
    private Integer idLiquidacion;
    private String codigoLiquidacion;
    private Integer idExpediente;
    private String codigoExpediente;
    private String numeroDam;

    // Desglose de cobranza consolidada
    private BigDecimal totalTributosSunat;
    private BigDecimal gastosAlmacenPuerto;
    private BigDecimal fleteTransporteLocal;
    private BigDecimal honorariosAgencia;
    private BigDecimal subtotalImponible;
    private BigDecimal igvMonto;

    // Detracciones SPOT
    private Boolean aplicaDetraccion;
    private BigDecimal porcentajeDetraccion;
    private BigDecimal montoDetraccion;

    // Totales Finales
    private BigDecimal totalGeneralPen;
    private BigDecimal totalGeneralUsd;
    private BigDecimal tipoCambio;

    // Cobranza y Facturación
    private String estadoPago; // PENDIENTE, EN_CONCILIACION, PAGADO
    private String numeroFactura;
    private LocalDate fechaEmision;
    private LocalDateTime fechaPago;

    // Datos bancarios
    private String metodoPago;
    private String bancoDestino;
    private String numeroOperacion;
}

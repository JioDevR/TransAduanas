package pe.sga.dto.portal;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalResumenFinancieroDTO {
    // Totales en Dólares (USD)
    private BigDecimal totalCotizadoUsd;
    private BigDecimal totalAbonadoUsd;
    private BigDecimal saldoPendienteUsd;

    // Totales en Soles (PEN) de Liquidaciones
    private BigDecimal totalLiquidacionesPen;
    private BigDecimal totalLiquidacionesUsd;
    private BigDecimal totalDetraccionesPen;

    // Contadores
    private Long cotizacionesActivas;
    private Long expedientesEnTransito;
    private Long pagosPendientesRevision;
}

package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanzasMetricasResponseDTO {

    private BigDecimal totalCobradoPen;
    private BigDecimal totalCobradoUsd;
    private BigDecimal cuentasPorCobrarPen;
    private BigDecimal cuentasPorCobrarUsd;
    private BigDecimal totalTributosSunatPen;
    private BigDecimal totalDetraccionesPen;

    private Long totalLiquidaciones;
    private Long liquidacionesPendientes;
    private Long liquidacionesPagadas;
    private Long liquidacionesAnuladas;

    private BigDecimal tipoCambioOficial;
}

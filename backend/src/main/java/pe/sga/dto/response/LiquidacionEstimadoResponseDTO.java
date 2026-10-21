package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionEstimadoResponseDTO {

    private Integer idExpediente;
    private String codigoExpediente;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;
    private String numeroDam;
    private String canalSunat;

    // Valores sugeridos
    private BigDecimal totalTributosSunatEstimado;
    private BigDecimal gastosAlmacenPuertoEstimado;
    private BigDecimal fleteTransporteLocalEstimado;
    private BigDecimal honorariosAgenciaEstimado;
    private BigDecimal tipoCambioSugerido;

    // Precálculos
    private BigDecimal subtotalImponible;
    private BigDecimal igvMonto;
    private Boolean aplicaDetraccion;
    private BigDecimal porcentajeDetraccion;
    private BigDecimal montoDetraccion;
    private BigDecimal totalGeneralPen;
    private BigDecimal totalGeneralUsd;
}

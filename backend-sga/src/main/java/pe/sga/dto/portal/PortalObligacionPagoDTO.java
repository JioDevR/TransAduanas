package pe.sga.dto.portal;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalObligacionPagoDTO {

    // "ANTICIPO_COTIZACION" o "EXPEDIENTE_HEREDADO"
    private String tipoObligacion;

    private Integer idReferencia;
    private Integer idCotizacion;
    private Integer idExpediente;

    private String codigo;
    private String codigoCotizacion;
    private String codigoExpediente;

    private String descripcion;
    private String numeroDam;
    private String canalSunat;

    @Builder.Default
    private BigDecimal totalContratadoUsd = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalAmortizadoUsd = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal saldoPendienteUsd = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal montoSugeridoAnticipoUsd = BigDecimal.ZERO;

    private String estadoPago;
    private boolean tieneExpediente;
    private LocalDateTime fechaReferencia;
}

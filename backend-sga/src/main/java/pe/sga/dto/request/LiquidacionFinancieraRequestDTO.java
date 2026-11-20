package pe.sga.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionFinancieraRequestDTO {

    @NotNull(message = "El idExpediente es obligatorio")
    private Integer idExpediente;

    @Builder.Default
    @DecimalMin(value = "0.0", inclusive = true, message = "Los tributos no pueden ser negativos")
    private BigDecimal totalTributosSunat = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", inclusive = true, message = "Los gastos de almacén no pueden ser negativos")
    private BigDecimal gastosAlmacenPuerto = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", inclusive = true, message = "El flete no puede ser negativo")
    private BigDecimal fleteTransporteLocal = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", inclusive = true, message = "Los honorarios no pueden ser negativos")
    private BigDecimal honorariosAgencia = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.1", inclusive = true, message = "El tipo de cambio debe ser mayor a 0")
    private BigDecimal tipoCambio = new BigDecimal("3.7500");

    @Builder.Default
    private Boolean aplicaDetraccion = true;

    @Builder.Default
    private BigDecimal porcentajeDetraccion = new BigDecimal("12.00");

    @Size(max = 30, message = "El número de factura no puede exceder 30 caracteres")
    private String numeroFactura;

    private Integer idDocumentoFactura;

    private LocalDate fechaEmision;

    private String observaciones;
}

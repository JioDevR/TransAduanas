package pe.sga.dto.portal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalRegistroVoucherDTO {

    // Tipo de obligación: ANTICIPO_COTIZACION o LIQUIDACION_EXPEDIENTE
    @NotBlank(message = "El tipo de obligación es requerido (ANTICIPO_COTIZACION o LIQUIDACION_EXPEDIENTE)")
    private String tipoObligacion;

    // ID de la cotización o liquidación a pagar
    @NotNull(message = "El ID de la obligación (idCotizacion o idLiquidacion) es requerido")
    private Integer idReferencia;

    @NotNull(message = "El monto abonado es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoAbonado;

    @Builder.Default
    private String moneda = "USD";

    @NotBlank(message = "El método de pago es requerido (TRANSFERENCIA, DEPOSITO, etc.)")
    private String metodoPago;

    private String bancoOrigen;

    @NotBlank(message = "El banco de destino es requerido")
    private String bancoDestino;

    @NotBlank(message = "El número de operación bancaria es requerido")
    private String numeroOperacion;

    @NotNull(message = "La fecha de la operación bancaria es requerida")
    private LocalDate fechaOperacion;

    private String observaciones;

    private Integer idDocumentoComprobante;
}

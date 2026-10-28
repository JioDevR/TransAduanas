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
public class PortalCorreccionPagoDTO {

    @NotNull(message = "El monto abonado es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoAbonado;

    @Builder.Default
    private String moneda = "USD";

    @NotBlank(message = "El método de pago es requerido")
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

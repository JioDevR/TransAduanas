package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionPagoDTO {

    @NotBlank(message = "El método de pago es obligatorio")
    @Size(max = 50, message = "El método de pago no puede exceder 50 caracteres")
    private String metodoPago;

    @Size(max = 80, message = "El banco de destino no puede exceder 80 caracteres")
    private String bancoDestino;

    @NotBlank(message = "El número de operación es obligatorio")
    @Size(max = 50, message = "El número de operación no puede exceder 50 caracteres")
    private String numeroOperacion;

    @Size(max = 30, message = "El número de factura no puede exceder 30 caracteres")
    private String numeroFactura;

    private Integer idDocumentoVoucher;

    private LocalDateTime fechaPago;

    private String observaciones;
}

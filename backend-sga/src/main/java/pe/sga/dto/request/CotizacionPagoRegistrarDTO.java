package pe.sga.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.sga.entity.enums.TipoAbonoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionPagoRegistrarDTO {

    @NotNull(message = "El ID de la cotización es obligatorio")
    private Integer idCotizacion;

    @NotNull(message = "El tipo de abono es obligatorio")
    private TipoAbonoCotizacion tipoAbono;

    @NotNull(message = "El monto abonado es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoAbonado;

    @NotBlank(message = "La moneda es obligatoria")
    private String moneda;

    private BigDecimal tipoCambio;

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;

    private String bancoOrigen;

    @NotBlank(message = "El banco de destino es obligatorio")
    private String bancoDestino;

    @NotBlank(message = "El número de operación es obligatorio")
    private String numeroOperacion;

    @NotNull(message = "La fecha de operación es obligatoria")
    private LocalDate fechaOperacion;

    private Integer idDocumentoComprobante;
}

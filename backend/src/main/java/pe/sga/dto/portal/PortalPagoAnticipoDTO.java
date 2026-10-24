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
public class PortalPagoAnticipoDTO {
    private Integer idPago;
    private Integer idCotizacion;
    private String codigoCotizacion;
    private String codigoPago;
    private String tipoAbono;
    private BigDecimal montoAbonado;
    private String moneda;
    private BigDecimal tipoCambio;
    private BigDecimal montoEquivalentePen;
    private String metodoPago;
    private String bancoOrigen;
    private String bancoDestino;
    private String numeroOperacion;
    private LocalDate fechaOperacion;
    private String estadoPago; // PENDIENTE_REVISION, VALIDADO, RECHAZADO
    private String observacionFinanzas;
    private LocalDateTime fechaRegistro;
}

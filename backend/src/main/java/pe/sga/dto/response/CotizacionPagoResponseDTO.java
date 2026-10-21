package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.TipoAbonoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionPagoResponseDTO {

    private Integer idPago;
    private Integer idCotizacion;
    private String codigoCotizacion;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;
    private BigDecimal totalCotizado;
    private String codigoPago;
    private TipoAbonoCotizacion tipoAbono;
    private BigDecimal montoAbonado;
    private String moneda;
    private BigDecimal tipoCambio;
    private BigDecimal montoEquivalentePen;
    private String metodoPago;
    private String bancoOrigen;
    private String bancoDestino;
    private String numeroOperacion;
    private LocalDate fechaOperacion;

    // Comprobante
    private Integer idDocumentoComprobante;
    private String documentoComprobanteNombre;
    private String documentoComprobanteUrl;

    // Validación Finanzas
    private EstadoPagoCotizacion estadoPago;
    private Integer idUsuarioFinanzas;
    private String usuarioFinanzasNombre;
    private LocalDateTime fechaValidacion;
    private String observacionFinanzas;

    // Registro
    private Integer idUsuarioRegistro;
    private String usuarioRegistroNombre;
    private LocalDateTime fechaRegistro;
}

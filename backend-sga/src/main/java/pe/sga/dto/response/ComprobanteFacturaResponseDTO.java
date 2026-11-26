package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteFacturaResponseDTO {

    private Integer idComprobante;
    private String tipoComprobante;
    private String serie;
    private Integer correlativo;
    private String numeroCompleto;

    private String origenTipo;
    private Integer idPagoCotizacion;
    private String codigoPago;
    private Integer idCotizacion;
    private String codigoCotizacion;
    private Integer idLiquidacion;
    private String codigoLiquidacion;

    private Integer idCliente;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;
    private String clienteDireccion;

    private String codigoReferencia;
    private String concepto;
    private String moneda;
    private BigDecimal tipoCambio;
    private BigDecimal inafecto;
    private BigDecimal subtotalGravado;
    private BigDecimal igvMonto;
    private BigDecimal totalComprobante;
    private BigDecimal totalEquivalentePen;

    private String bancoDestino;
    private String numeroOperacion;

    private String estado;
    private LocalDateTime fechaEmision;
    private Integer idUsuarioEmisor;
    private String usuarioEmisorNombre;
    private String observaciones;
}

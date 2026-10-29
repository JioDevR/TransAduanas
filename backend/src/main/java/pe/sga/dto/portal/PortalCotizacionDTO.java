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
public class PortalCotizacionDTO {
    private Integer idCotizacion;
    private String codigoCotizacion;
    private Integer idCliente;
    private String razonSocialCliente;
    private String rucCliente;

    // Mercancía y Carga
    private Integer idMercancia;
    private String codigoMercancia;
    private String nombreMercancia;
    private Integer idTipoCarga;
    private String nombreTipoCarga;

    // Régimen y Rutas
    private String regimenAduanero;
    private Integer idPuertoEnvio;
    private String nombrePuertoEnvio;
    private Integer idPuertoDestino;
    private String nombrePuertoDestino;
    private Boolean servicioTransporte;
    private String origenDepartamento;
    private String origenProvincia;
    private String destinoDepartamento;
    private String destinoProvincia;

    // Costos y Liquidación Estimada (USD)
    private BigDecimal valorFob;
    private BigDecimal montoFlete;
    private BigDecimal montoSeguroEstimado;
    private BigDecimal montoHonorarios;
    private BigDecimal tasaAdvalorem;
    private BigDecimal tasaPercepcion;
    private BigDecimal montoTributos;
    private BigDecimal totalCotizado;
    private Integer validezDias;
    private LocalDate fechaEmision;

    // Estados
    private String estado;
    private LocalDateTime fechaRevision;
    private String motivoObservacion;
    private String observaciones;
    private Boolean aceptadaPorCliente;

    // Pagos
    private String estadoPago;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
}

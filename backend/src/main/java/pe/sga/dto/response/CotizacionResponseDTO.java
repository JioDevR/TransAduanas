package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.EstadoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionResponseDTO {

    private Integer idCotizacion;
    private String codigoCotizacion;
    private Integer idCliente;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;
    private Integer idUsuarioComercial;
    private String usuarioComercialNombre;

    // Mercancía y Carga
    private Integer idMercancia;
    private String mercanciaCodigo;
    private String mercanciaNombre;
    private Integer idTipoCarga;
    private String tipoCargaNombre;

    // Régimen y Puertos
    private String regimenAduanero;
    private Integer idPuertoEnvio;
    private String puertoEnvioNombre;
    private Integer idPuertoDestino;
    private String puertoDestinoNombre;
    private Boolean servicioTransporte;
    private String origenDepartamento;
    private String origenProvincia;
    private String destinoDepartamento;
    private String destinoProvincia;
    private BigDecimal costoTransporteInterno;

    // Costos y Fechas
    private LocalDate fechaEmision;
    private Integer validezDias;
    private BigDecimal valorFob;
    private BigDecimal montoFleteEstimado;
    private BigDecimal montoSeguroEstimado;
    private BigDecimal montoHonorariosEstimado;
    private BigDecimal tasaAdValorem;
    private BigDecimal tasaPercepcion;
    private BigDecimal montoTributosEstimado;
    private BigDecimal totalCotizado;

    // Revisión Agente Aduanero
    private EstadoCotizacion estado;
    private Integer idAgenteRevisor;
    private String agenteRevisorNombre;
    private LocalDateTime fechaRevision;
    private String motivoObservacion;
    private String observaciones;

    // Enlace a Operación Aduanera (Expediente)
    private Boolean tieneExpediente;
    private Integer idExpediente;
    private String codigoExpediente;

    // Control Financiero Pre-Expediente
    private String estadoPago;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private Boolean tienePagoAprobado;
    private Boolean habilitadoParaExpediente;
}

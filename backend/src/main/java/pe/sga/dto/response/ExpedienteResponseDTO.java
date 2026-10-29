package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteResponseDTO {

    private Integer idExpediente;
    private String codigoExpediente;

    // Cotización origen
    private Integer idCotizacion;
    private String codigoCotizacion;
    private String regimenAduanero;

    // Cliente
    private Integer idCliente;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;

    // Agente Aduanero Asignado
    private Integer idAgenteAduanero;
    private String agenteAduaneroNombre;
    private LocalDateTime fechaAsignacion;

    // Contacto Exterior
    private String contactoExteriorNombre;
    private String contactoExteriorTelefono;
    private String contactoExteriorCorreo;

    // Tránsito y Aduanas
    private LocalDate fechaZarpeEtd;
    private LocalDate fechaLlegadaEta;
    private LocalDateTime fechaArriboAduana;

    private String numeroDam;
    private CanalSunat canalSunat;
    private LocalDateTime fechaAsignacionCanal;

    private Boolean notificadoAgente;
    private LocalDateTime fechaNotificacion;
    private String detalleInspeccion;
    private LocalDateTime fechaLevanteAutorizacion;

    // Contenedor y Nave
    private String numeroContenedor;
    private String precintoAduanero;
    private String naveViaje;

    // Estado del Expediente
    private EstadoExpediente estadoExpediente;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String observaciones;

    // Mercancía y Carga
    private Integer idMercancia;
    private String mercanciaCodigo;
    private String mercanciaNombre;
    private Integer idTipoCarga;
    private String tipoCargaNombre;
    private String tipoCargaDescripcion;

    // Modalidad y Puertos
    private String modalidad;
    private Integer idPuertoEnvio;
    private String puertoEnvioNombre;
    private String puertoEnvioCodigo;
    private String puertoEnvioPais;
    private String puertoEnvioTipo;
    private Integer idPuertoDestino;
    private String puertoDestinoNombre;
    private String puertoDestinoCodigo;
    private String puertoDestinoPais;
    private String puertoDestinoTipo;

    // Logística y Ruta Terrestre
    private String origenDepartamento;
    private String origenProvincia;
    private String destinoDepartamento;
    private String destinoProvincia;
    private BigDecimal costoTransporteInterno;

    // Valoración Económica y Tributos
    private BigDecimal valorFob;
    private BigDecimal montoFleteEstimado;
    private BigDecimal montoSeguroEstimado;
    private BigDecimal montoHonorariosEstimado;
    private BigDecimal tasaAdValorem;
    private BigDecimal tasaPercepcion;
    private BigDecimal montoTributosEstimado;
    private BigDecimal totalCotizado;

    // Control Financiero Pre-Expediente
    private String estadoPago;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private String usuarioComercialNombre;

    // Transporte y Despacho
    private Boolean servicioTransporte;
    private Integer idDespachoTransporte;
    private String codigoTrackingTransporte;
    private String estadoTransporte;
}


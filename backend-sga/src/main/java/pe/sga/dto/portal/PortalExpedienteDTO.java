package pe.sga.dto.portal;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.sga.dto.response.ExpedienteEventoDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalExpedienteDTO {
    private Integer idExpediente;
    private String codigoExpediente;
    private Integer idCotizacion;
    private String codigoCotizacion;
    private String regimenAduanero;
    private Boolean servicioTransporte;
    private Integer idCliente;
    private String razonSocialCliente;
    private String rucCliente;

    // Mercancía
    private Integer idMercancia;
    private String codigoMercancia;
    private String nombreMercancia;
    private Integer idTipoCarga;
    private String nombreTipoCarga;

    // Agente Asignado
    private Integer idAgenteAduanero;
    private String agenteAduaneroNombre;
    private Boolean pendienteAsignacionAgente;

    // Contacto Exterior
    private String contactoExteriorNombre;
    private String contactoExteriorTelefono;
    private String contactoExteriorCorreo;

    // DAM y SUNAT
    private String numeroDam;
    private String canalSunat; // VERDE, NARANJA, ROJO, PENDIENTE
    private LocalDateTime fechaAsignacionCanal;
    private LocalDateTime fechaLevanteAutorizacion;
    private String detalleInspeccion;

    // Nave, Contenedor, Vuelo y Tránsito
    private String naveViaje;
    private String tipoTransporteInternacional;
    private String numeroVuelo;
    private String numeroContenedor;
    private String precintoAduanero;
    private LocalDate fechaZarpeEtd;
    private LocalDate fechaLlegadaEta;
    private LocalDateTime fechaArriboAduana;

    // Hitos Operativos & Conformidades
    private Boolean enEsperaTransito;
    private Boolean enRevisionAduana;
    private Integer idDocumentoConformidadDestino;
    private Boolean verificacionOrigenOk;
    private LocalDateTime fechaVerificacionOrigen;
    private Boolean cuadrillaSolicitada;
    private String cuadrillaDetalle;
    private LocalDateTime fechaCitaInspeccion;
    private String codigoCitaInspeccion;
    private Boolean revisionRojoFinalizada;
    private Boolean envioSunatSimuladoOk;
    private String codigoTransmisionSunat;
    private LocalDateTime fechaEnvioSunat;
    private Boolean entregadoAlmacenVerde;
    private LocalDateTime fechaEntregaAlmacenVerde;
    private String informeConformidadTexto;
    private LocalDateTime fechaInformeConformidad;
    private String receptorConformidadNombre;
    private String receptorConformidadDni;
    private Integer idDocumentoConformidadInforme;
    private List<ExpedienteEventoDTO> eventos;

    // Estado del Expediente
    private String estadoExpediente; // ASIGNADO, NUMERADO, EN_CANAL, LEVANTE_AUTORIZADO, CUSTODIA_ALMACEN, EN_DESPACHO, FINALIZADO
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String observaciones;

    // Datos de Almacén Temporal (Depósito)
    private Boolean tieneAlmacen;
    private String depositoTemporal;
    private String ubicacionBahia;
    private Integer diasLibresAlmacen;
    private LocalDate fechaLimiteRetiro;
    private Long diasRestantesAlmacen; // Días que le quedan antes de cobrar sobreestadía
    private Integer cantidadBultos;
    private BigDecimal pesoBrutoKg;
    private String estadoAlmacen;

    // Datos de Transporte Externo (Solicitud registrada)
    private Boolean tieneTransporte;
    private String tipoTraslado;
    private String direccionPartida;
    private String direccionLlegada;
    private String distritoLlegada;
    private String contactoRecepcionTransporte;
    private LocalDateTime fechaProgramadaTransporte;
    private String observacionesTransporte;
    private LocalDateTime fechaSolicitudTransporte;
    private String estadoTransporte;
}

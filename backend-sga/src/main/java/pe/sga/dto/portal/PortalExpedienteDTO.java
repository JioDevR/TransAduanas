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
public class PortalExpedienteDTO {
    private Integer idExpediente;
    private String codigoExpediente;
    private Integer idCotizacion;
    private String codigoCotizacion;
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
    private String agenteAduaneroNombre;

    // DAM y SUNAT
    private String numeroDam;
    private String canalSunat; // VERDE, NARANJA, ROJO, PENDIENTE
    private LocalDateTime fechaAsignacionCanal;
    private LocalDateTime fechaLevanteAutorizacion;
    private String detalleInspeccion;

    // Nave, Contenedor y Tránsito
    private String naveViaje;
    private String numeroContenedor;
    private String precintoAduanero;
    private LocalDate fechaZarpeEtd;
    private LocalDate fechaLlegadaEta;
    private LocalDateTime fechaArriboAduana;

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

    // Datos de Transporte y Despacho Local
    private Boolean tieneTransporte;
    private String codigoTracking;
    private String placaTracto;
    private String conductorNombre;
    private String direccionPartida;
    private String direccionLlegada;
    private String distritoLlegada;
    private LocalDateTime fechaProgramadaTransporte;
    private String estadoTransporte;
}

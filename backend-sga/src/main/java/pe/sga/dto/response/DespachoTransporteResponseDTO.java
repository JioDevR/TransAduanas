package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.entity.enums.TipoTraslado;
import pe.sga.entity.enums.TipoVehiculo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoTransporteResponseDTO {

    private Integer idDespachoTransporte;
    private String codigoTracking;
    private TipoTraslado tipoTraslado;

    // Expediente asociado
    private Integer idExpediente;
    private String codigoExpediente;
    private String numeroDam;
    private String canalSunat;
    private String clienteRazonSocial;
    private String clienteNumeroDocumento;
    private String clienteTelefono;
    private String numeroContenedor;
    private String precintoAduanero;
    private String mercanciaNombre;

    // Recursos Asignados 3NF
    private Integer idConductor;
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    private Integer idUnidad;
    private String unidadPlacaTracto;
    private String unidadPlacaCarreta;
    private String unidadMarcaModelo;
    private TipoVehiculo unidadTipoVehiculo;
    private BigDecimal unidadCapacidadKg;
    private Integer unidadKilometrajeActual;

    // Documentación y Ruta
    private String guiaRemisionTransporte;
    private Integer idDocumentoRemision;
    private String nombreDocumentoRemision;

    private String direccionPartida;
    private String direccionLlegada;
    private String distritoLlegada;
    private String contactoRecepcion;
    private String receptorNombre;
    private String receptorDni;

    // Control de Tiempos
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaSalidaRuta;
    private LocalDateTime fechaEntregaConforme;

    // Estado
    private EstadoTransporte estadoTransporte;
    private String detalleIncidencia;
    private String observaciones;
}

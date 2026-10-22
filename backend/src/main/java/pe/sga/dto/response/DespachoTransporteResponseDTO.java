package pe.sga.dto.response;

import lombok.*;

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
    private String tipoTraslado;

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
    private Integer idUsuarioConductor;
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    private Integer idUnidad;
    private String unidadPlacaTracto;
    private String unidadPlacaCarreta;
    private String unidadMarcaModelo;
    private String unidadTipoVehiculo;
    private BigDecimal unidadCapacidadKg;

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
    private String estadoTransporte;
    private String detalleIncidencia;
    private String observaciones;
}

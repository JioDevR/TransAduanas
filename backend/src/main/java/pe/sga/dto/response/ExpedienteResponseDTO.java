package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

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
}

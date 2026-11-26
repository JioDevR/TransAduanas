package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expediente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente")
    private Integer idExpediente;

    @Column(name = "codigo_expediente", nullable = false, unique = true, length = 20)
    private String codigoExpediente;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cotizacion", nullable = false, unique = true)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mercancia")
    private CatalogoMercancia mercancia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_carga")
    private CatalogoTipoCarga tipoCarga;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agente_aduanero", nullable = true)
    private Usuario agenteAduanero;

    @Column(name = "fecha_asignacion")
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    // Contacto Exterior (Proveedor en origen o Comprador en destino)
    @Column(name = "contacto_exterior_nombre", length = 150)
    private String contactoExteriorNombre;

    @Column(name = "contacto_exterior_telefono", length = 30)
    private String contactoExteriorTelefono;

    @Column(name = "contacto_exterior_correo", length = 100)
    private String contactoExteriorCorreo;

    // Fechas de Tránsito Internacional
    @Column(name = "fecha_zarpe_etd")
    private LocalDate fechaZarpeEtd;

    @Column(name = "fecha_llegada_eta")
    private LocalDate fechaLlegadaEta;

    @Column(name = "fecha_arribo_aduana")
    private LocalDateTime fechaArriboAduana;

    // Trámite Aduanero SUNAT
    @Column(name = "numero_dam", unique = true, length = 30)
    private String numeroDam;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_sunat", nullable = false, length = 20)
    @Builder.Default
    private CanalSunat canalSunat = CanalSunat.PENDIENTE;

    @Column(name = "fecha_asignacion_canal")
    private LocalDateTime fechaAsignacionCanal;

    // Inspección y Notificación
    @Column(name = "notificado_agente", nullable = false)
    @Builder.Default
    private Boolean notificadoAgente = false;

    @Column(name = "fecha_notificacion")
    private LocalDateTime fechaNotificacion;

    @Column(name = "detalle_inspeccion", columnDefinition = "TEXT")
    private String detalleInspeccion;

    @Column(name = "fecha_levante_autorizacion")
    private LocalDateTime fechaLevanteAutorizacion;

    // Carga y Manifiesto
    @Column(name = "numero_contenedor", length = 30)
    private String numeroContenedor;

    @Column(name = "precinto_aduanero", length = 50)
    private String precintoAduanero;

    @Column(name = "nave_viaje", length = 100)
    private String naveViaje;

    @Column(name = "tipo_transporte_internacional", length = 30)
    private String tipoTransporteInternacional;

    @Column(name = "numero_vuelo", length = 60)
    private String numeroVuelo;

    // Marcación de Estados Operativos y Canales SUNAT
    @Column(name = "en_espera_transito", nullable = false)
    @Builder.Default
    private Boolean enEsperaTransito = false;

    @Column(name = "en_revision_aduana", nullable = false)
    @Builder.Default
    private Boolean enRevisionAduana = false;

    @Column(name = "id_documento_conformidad_destino")
    private Integer idDocumentoConformidadDestino;

    @Column(name = "verificacion_origen_ok", nullable = false)
    @Builder.Default
    private Boolean verificacionOrigenOk = false;

    @Column(name = "fecha_verificacion_origen")
    private LocalDateTime fechaVerificacionOrigen;

    @Column(name = "cuadrilla_solicitada", nullable = false)
    @Builder.Default
    private Boolean cuadrillaSolicitada = false;

    @Column(name = "cuadrilla_detalle", length = 200)
    private String cuadrillaDetalle;

    @Column(name = "fecha_cita_inspeccion")
    private LocalDateTime fechaCitaInspeccion;

    @Column(name = "codigo_cita_inspeccion", length = 50)
    private String codigoCitaInspeccion;

    @Column(name = "revision_rojo_finalizada", nullable = false)
    @Builder.Default
    private Boolean revisionRojoFinalizada = false;

    @Column(name = "envio_sunat_simulado_ok", nullable = false)
    @Builder.Default
    private Boolean envioSunatSimuladoOk = false;

    @Column(name = "codigo_transmision_sunat", length = 60)
    private String codigoTransmisionSunat;

    @Column(name = "fecha_envio_sunat")
    private LocalDateTime fechaEnvioSunat;

    @Column(name = "entregado_almacen_verde", nullable = false)
    @Builder.Default
    private Boolean entregadoAlmacenVerde = false;

    @Column(name = "fecha_entrega_almacen_verde")
    private LocalDateTime fechaEntregaAlmacenVerde;

    @Column(name = "informe_conformidad_texto", columnDefinition = "TEXT")
    private String informeConformidadTexto;

    @Column(name = "fecha_informe_conformidad")
    private LocalDateTime fechaInformeConformidad;

    @Column(name = "receptor_conformidad_nombre", length = 150)
    private String receptorConformidadNombre;

    @Column(name = "receptor_conformidad_dni", length = 30)
    private String receptorConformidadDni;

    @Column(name = "id_documento_informe_conformidad")
    private Integer idDocumentoConformidadInforme;

    // Control y Auditoría
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_expediente", nullable = false, length = 30)
    @Builder.Default
    private EstadoExpediente estadoExpediente = EstadoExpediente.ASIGNADO;

    @Column(name = "fecha_apertura", nullable = false)
    @Builder.Default
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

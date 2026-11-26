package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SecondaryRow;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expediente")
@SecondaryTables({
        @SecondaryTable(name = "expediente_contacto_exterior", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_transito_internacional", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_canal_sunat", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_inspeccion_rojo", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_transmision_naranja", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_conformidad_entrega", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente")),
        @SecondaryTable(name = "expediente_solicitud_transporte", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_expediente"))
})
@SecondaryRow(table = "expediente_contacto_exterior", optional = true)
@SecondaryRow(table = "expediente_transito_internacional", optional = true)
@SecondaryRow(table = "expediente_canal_sunat", optional = true)
@SecondaryRow(table = "expediente_inspeccion_rojo", optional = true)
@SecondaryRow(table = "expediente_transmision_naranja", optional = true)
@SecondaryRow(table = "expediente_conformidad_entrega", optional = true)
@SecondaryRow(table = "expediente_solicitud_transporte", optional = true)
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

    // Contacto Exterior (Satélite 6FN: expediente_contacto_exterior)
    @Column(table = "expediente_contacto_exterior", name = "contacto_exterior_nombre", length = 150)
    private String contactoExteriorNombre;

    @Column(table = "expediente_contacto_exterior", name = "contacto_exterior_telefono", length = 30)
    private String contactoExteriorTelefono;

    @Column(table = "expediente_contacto_exterior", name = "contacto_exterior_correo", length = 100)
    private String contactoExteriorCorreo;

    @Column(table = "expediente_contacto_exterior", name = "verificacion_origen_ok", nullable = false)
    @Builder.Default
    private Boolean verificacionOrigenOk = false;

    @Column(table = "expediente_contacto_exterior", name = "fecha_verificacion_origen")
    private LocalDateTime fechaVerificacionOrigen;

    // Tránsito Internacional y Manifiesto (Satélite 6FN: expediente_transito_internacional)
    @Column(table = "expediente_transito_internacional", name = "fecha_zarpe_etd")
    private LocalDate fechaZarpeEtd;

    @Column(table = "expediente_transito_internacional", name = "fecha_llegada_eta")
    private LocalDate fechaLlegadaEta;

    @Column(table = "expediente_transito_internacional", name = "fecha_arribo_aduana")
    private LocalDateTime fechaArriboAduana;

    @Column(table = "expediente_transito_internacional", name = "numero_contenedor", length = 30)
    private String numeroContenedor;

    @Column(table = "expediente_transito_internacional", name = "precinto_aduanero", length = 50)
    private String precintoAduanero;

    @Column(table = "expediente_transito_internacional", name = "nave_viaje", length = 100)
    private String naveViaje;

    @Column(table = "expediente_transito_internacional", name = "tipo_transporte_internacional", length = 30)
    private String tipoTransporteInternacional;

    @Column(table = "expediente_transito_internacional", name = "numero_vuelo", length = 60)
    private String numeroVuelo;

    // Trámite Aduanero SUNAT (Satélite 6FN: expediente_canal_sunat)
    @Column(table = "expediente_canal_sunat", name = "numero_dam", unique = true, length = 30)
    private String numeroDam;

    @Enumerated(EnumType.STRING)
    @Column(table = "expediente_canal_sunat", name = "canal_sunat", nullable = false, length = 20)
    @Builder.Default
    private CanalSunat canalSunat = CanalSunat.PENDIENTE;

    @Column(table = "expediente_canal_sunat", name = "fecha_asignacion_canal")
    private LocalDateTime fechaAsignacionCanal;

    @Column(table = "expediente_canal_sunat", name = "notificado_agente", nullable = false)
    @Builder.Default
    private Boolean notificadoAgente = false;

    @Column(table = "expediente_canal_sunat", name = "fecha_notificacion")
    private LocalDateTime fechaNotificacion;

    @Column(table = "expediente_canal_sunat", name = "fecha_levante_autorizacion")
    private LocalDateTime fechaLevanteAutorizacion;

    // Inspección Canal Rojo (Satélite 6FN: expediente_inspeccion_rojo)
    @Column(table = "expediente_inspeccion_rojo", name = "detalle_inspeccion", columnDefinition = "TEXT")
    private String detalleInspeccion;

    @Column(table = "expediente_inspeccion_rojo", name = "cuadrilla_solicitada", nullable = false)
    @Builder.Default
    private Boolean cuadrillaSolicitada = false;

    @Column(table = "expediente_inspeccion_rojo", name = "cuadrilla_detalle", length = 200)
    private String cuadrillaDetalle;

    @Column(table = "expediente_inspeccion_rojo", name = "fecha_cita_inspeccion")
    private LocalDateTime fechaCitaInspeccion;

    @Column(table = "expediente_inspeccion_rojo", name = "codigo_cita_inspeccion", length = 50)
    private String codigoCitaInspeccion;

    @Column(table = "expediente_inspeccion_rojo", name = "revision_rojo_finalizada", nullable = false)
    @Builder.Default
    private Boolean revisionRojoFinalizada = false;

    // Transmisión Canal Naranja (Satélite 6FN: expediente_transmision_naranja)
    @Column(table = "expediente_transmision_naranja", name = "envio_sunat_simulado_ok", nullable = false)
    @Builder.Default
    private Boolean envioSunatSimuladoOk = false;

    @Column(table = "expediente_transmision_naranja", name = "codigo_transmision_sunat", length = 60)
    private String codigoTransmisionSunat;

    @Column(table = "expediente_transmision_naranja", name = "fecha_envio_sunat")
    private LocalDateTime fechaEnvioSunat;

    // Entrega y Conformidad (Satélite 6FN: expediente_conformidad_entrega)
    @Column(table = "expediente_conformidad_entrega", name = "id_documento_conformidad_destino")
    private Integer idDocumentoConformidadDestino;

    @Column(table = "expediente_conformidad_entrega", name = "entregado_almacen_verde", nullable = false)
    @Builder.Default
    private Boolean entregadoAlmacenVerde = false;

    @Column(table = "expediente_conformidad_entrega", name = "fecha_entrega_almacen_verde")
    private LocalDateTime fechaEntregaAlmacenVerde;

    @Column(table = "expediente_conformidad_entrega", name = "informe_conformidad_texto", columnDefinition = "TEXT")
    private String informeConformidadTexto;

    @Column(table = "expediente_conformidad_entrega", name = "fecha_informe_conformidad")
    private LocalDateTime fechaInformeConformidad;

    @Column(table = "expediente_conformidad_entrega", name = "receptor_conformidad_nombre", length = 150)
    private String receptorConformidadNombre;

    @Column(table = "expediente_conformidad_entrega", name = "receptor_conformidad_dni", length = 30)
    private String receptorConformidadDni;

    @Column(table = "expediente_conformidad_entrega", name = "id_documento_informe_conformidad")
    private Integer idDocumentoConformidadInforme;

    // Solicitud de transporte externo (Satélite 6FN: expediente_solicitud_transporte)
    @Column(table = "expediente_solicitud_transporte", name = "transporte_solicitado", nullable = false)
    @Builder.Default
    private Boolean transporteSolicitado = false;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_tipo_traslado", length = 30)
    private String transporteTipoTraslado;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_direccion_partida", length = 255)
    private String transporteDireccionPartida;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_direccion_llegada", length = 255)
    private String transporteDireccionLlegada;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_distrito_llegada", length = 100)
    private String transporteDistritoLlegada;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_contacto_recepcion", length = 100)
    private String transporteContactoRecepcion;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_fecha_programada")
    private LocalDateTime transporteFechaProgramada;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_observaciones", columnDefinition = "TEXT")
    private String transporteObservaciones;

    @Column(table = "expediente_solicitud_transporte", name = "transporte_fecha_solicitud")
    private LocalDateTime transporteFechaSolicitud;

    // Marcación núcleo y Control y Auditoría
    @Column(name = "en_espera_transito", nullable = false)
    @Builder.Default
    private Boolean enEsperaTransito = false;

    @Column(name = "en_revision_aduana", nullable = false)
    @Builder.Default
    private Boolean enRevisionAduana = false;

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

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agente_aduanero", nullable = false)
    private Usuario agenteAduanero;

    @Column(name = "fecha_asignacion", nullable = false)
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

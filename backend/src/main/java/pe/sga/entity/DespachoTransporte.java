package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "despacho_transporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despacho_transporte")
    private Integer idDespachoTransporte;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_expediente", nullable = false, unique = true)
    private Expediente expediente;

    @Column(name = "codigo_tracking", nullable = false, unique = true, length = 30)
    private String codigoTracking;

    @Column(name = "tipo_traslado", nullable = false, length = 30)
    @Builder.Default
    private String tipoTraslado = "ENTREGA_IMPORTACION";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_conductor")
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_unidad")
    private UnidadTransporte unidad;

    @Column(name = "guia_remision_transporte", length = 30)
    private String guiaRemisionTransporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_remision")
    private DocumentoAdjunto documentoRemision;

    @Column(name = "direccion_partida", nullable = false, length = 255)
    private String direccionPartida;

    @Column(name = "direccion_llegada", nullable = false, length = 255)
    private String direccionLlegada;

    @Column(name = "distrito_llegada", nullable = false, length = 100)
    private String distritoLlegada;

    @Column(name = "contacto_recepcion", length = 100)
    private String contactoRecepcion;

    @Column(name = "receptor_nombre", length = 120)
    private String receptorNombre;

    @Column(name = "receptor_dni", length = 15)
    private String receptorDni;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_salida_ruta")
    private LocalDateTime fechaSalidaRuta;

    @Column(name = "fecha_entrega_conforme")
    private LocalDateTime fechaEntregaConforme;

    @Column(name = "estado_transporte", nullable = false, length = 30)
    @Builder.Default
    private String estadoTransporte = "PENDIENTE_ASIGNACION";

    @Column(name = "detalle_incidencia", columnDefinition = "TEXT")
    private String detalleIncidencia;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

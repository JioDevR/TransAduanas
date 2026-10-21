package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.EstadoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion")
    private Integer idCotizacion;

    @Column(name = "codigo_cotizacion", nullable = false, unique = true, length = 20)
    private String codigoCotizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_comercial", nullable = false)
    private Usuario usuarioComercial;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mercancia")
    private CatalogoMercancia mercancia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_carga")
    private CatalogoTipoCarga tipoCarga;

    @Column(name = "regimen_aduanero", nullable = false, length = 50)
    @Builder.Default
    private String regimenAduanero = "IMPORTACION_DEFINITIVA";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_puerto_envio")
    private CatalogoPuerto puertoEnvio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_puerto_destino")
    private CatalogoPuerto puertoDestino;

    @Column(name = "servicio_transporte", nullable = false)
    @Builder.Default
    private Boolean servicioTransporte = false;

    @Column(name = "origen_departamento", length = 60)
    private String origenDepartamento;

    @Column(name = "origen_provincia", length = 60)
    private String origenProvincia;

    @Column(name = "destino_departamento", length = 60)
    private String destinoDepartamento;

    @Column(name = "destino_provincia", length = 60)
    private String destinoProvincia;

    @Column(name = "costo_transporte_interno", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoTransporteInterno = BigDecimal.ZERO;

    @Column(name = "valor_fob", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorFob = BigDecimal.ZERO;

    @Column(name = "monto_flete_estimado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoFleteEstimado = BigDecimal.ZERO;

    @Column(name = "monto_seguro_estimado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoSeguroEstimado = BigDecimal.ZERO;

    @Column(name = "monto_honorarios_estimado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoHonorariosEstimado = BigDecimal.ZERO;

    @Column(name = "tasa_advalorem", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tasaAdValorem = BigDecimal.ZERO;

    @Column(name = "tasa_percepcion", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tasaPercepcion = BigDecimal.ZERO;

    @Column(name = "monto_tributos_estimado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoTributosEstimado = BigDecimal.ZERO;

    @Column(name = "total_cotizado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCotizado = BigDecimal.ZERO;

    @Column(name = "validez_dias", nullable = false)
    @Builder.Default
    private Integer validezDias = 15;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agente_revisor")
    private Usuario agenteRevisor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "motivo_observacion", columnDefinition = "TEXT")
    private String motivoObservacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "estado_pago", nullable = false, length = 25)
    @Builder.Default
    private String estadoPago = "SIN_PAGO";

    @Column(name = "total_abonado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAbonado = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoPendiente = BigDecimal.ZERO;
}

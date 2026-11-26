package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Integer idComprobante;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    @Builder.Default
    private String tipoComprobante = "FACTURA";

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    @Column(name = "numero_completo", nullable = false, unique = true, length = 25)
    private String numeroCompleto;

    @Column(name = "origen_tipo", nullable = false, length = 30)
    private String origenTipo; // ABONO_COTIZACION | LIQUIDACION_ADUANERA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_cotizacion")
    private CotizacionPago pagoCotizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_liquidacion")
    private LiquidacionFinanciera liquidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "cliente_razon_social", nullable = false, length = 200)
    private String clienteRazonSocial;

    @Column(name = "cliente_numero_documento", nullable = false, length = 25)
    private String clienteNumeroDocumento;

    @Column(name = "cliente_direccion", length = 255)
    private String clienteDireccion;

    @Column(name = "codigo_referencia", nullable = false, length = 60)
    private String codigoReferencia;

    @Column(name = "concepto", nullable = false, columnDefinition = "TEXT")
    private String concepto;

    @Column(name = "moneda", nullable = false, length = 5)
    @Builder.Default
    private String moneda = "PEN";

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal tipoCambio = new BigDecimal("3.7500");

    @Column(name = "inafecto", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal inafecto = BigDecimal.ZERO;

    @Column(name = "subtotal_gravado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalGravado = BigDecimal.ZERO;

    @Column(name = "igv_monto", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igvMonto = BigDecimal.ZERO;

    @Column(name = "total_comprobante", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalComprobante = BigDecimal.ZERO;

    @Column(name = "total_equivalente_pen", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEquivalentePen = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "EMITIDA";

    @Column(name = "fecha_emision", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_emisor")
    private Usuario usuarioEmisor;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

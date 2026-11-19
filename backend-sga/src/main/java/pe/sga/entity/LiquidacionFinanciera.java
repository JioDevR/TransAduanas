package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidacion_financiera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionFinanciera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liquidacion")
    private Integer idLiquidacion;

    @Column(name = "codigo_liquidacion", nullable = false, unique = true, length = 30)
    private String codigoLiquidacion;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_expediente", nullable = false, unique = true)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_liquidador")
    private Usuario usuarioLiquidador;

    @Column(name = "total_tributos_sunat", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTributosSunat = BigDecimal.ZERO;

    @Column(name = "gastos_almacen_puerto", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal gastosAlmacenPuerto = BigDecimal.ZERO;

    @Column(name = "flete_transporte_local", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fleteTransporteLocal = BigDecimal.ZERO;

    @Column(name = "honorarios_agencia", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal honorariosAgencia = BigDecimal.ZERO;

    @Column(name = "subtotal_imponible", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalImponible = BigDecimal.ZERO;

    @Column(name = "igv_monto", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igvMonto = BigDecimal.ZERO;

    @Column(name = "aplica_detraccion", nullable = false)
    @Builder.Default
    private Boolean aplicaDetraccion = true;

    @Column(name = "porcentaje_detraccion", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDetraccion = new BigDecimal("12.00");

    @Column(name = "monto_detraccion", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoDetraccion = BigDecimal.ZERO;

    @Column(name = "total_general_pen", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGeneralPen = BigDecimal.ZERO;

    @Column(name = "total_general_usd", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGeneralUsd = BigDecimal.ZERO;

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal tipoCambio = new BigDecimal("3.7500");

    @Column(name = "estado_pago", nullable = false, length = 20)
    @Builder.Default
    private String estadoPago = "PENDIENTE";

    @Column(name = "numero_factura", length = 30)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_factura")
    private DocumentoAdjunto documentoFactura;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "banco_destino", length = 80)
    private String bancoDestino;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_voucher")
    private DocumentoAdjunto documentoVoucher;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

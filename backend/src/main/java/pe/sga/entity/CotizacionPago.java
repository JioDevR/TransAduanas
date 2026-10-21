package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.TipoAbonoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizacion_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @Column(name = "codigo_pago", nullable = false, unique = true, length = 30)
    private String codigoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_abono", nullable = false, length = 30)
    @Builder.Default
    private TipoAbonoCotizacion tipoAbono = TipoAbonoCotizacion.ADELANTO;

    @Column(name = "monto_abonado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAbonado;

    @Column(name = "moneda", nullable = false, length = 5)
    @Builder.Default
    private String moneda = "USD";

    @Column(name = "tipo_cambio", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal tipoCambio = new BigDecimal("3.7500");

    @Column(name = "monto_equivalente_pen", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEquivalentePen;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Column(name = "banco_origen", length = 80)
    private String bancoOrigen;

    @Column(name = "banco_destino", nullable = false, length = 80)
    private String bancoDestino;

    @Column(name = "numero_operacion", nullable = false, length = 60)
    private String numeroOperacion;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_comprobante")
    private DocumentoAdjunto documentoComprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 25)
    @Builder.Default
    private EstadoPagoCotizacion estadoPago = EstadoPagoCotizacion.PENDIENTE_REVISION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_finanzas")
    private Usuario usuarioFinanzas;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "observacion_finanzas", columnDefinition = "TEXT")
    private String observacionFinanzas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}

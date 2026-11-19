package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "control_almacen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_control_almacen")
    private Integer idControlAlmacen;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_expediente", nullable = false, unique = true)
    private Expediente expediente;

    @Column(name = "deposito_temporal", nullable = false, length = 100)
    private String depositoTemporal;

    @Column(name = "ubicacion_bahia", length = 50)
    @Builder.Default
    private String ubicacionBahia = "Patio General";

    @Column(name = "tipo_ingreso", nullable = false, length = 30)
    @Builder.Default
    private String tipoIngreso = "DIRECTO_VERDE";

    @Column(name = "finalidad_almacenaje", nullable = false, length = 30)
    @Builder.Default
    private String finalidadAlmacenaje = "INGRESO_IMPORTACION";

    @Column(name = "fecha_ingreso", nullable = false)
    @Builder.Default
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @Column(name = "dias_libres_almacen", nullable = false)
    @Builder.Default
    private Integer diasLibresAlmacen = 15;

    @Column(name = "fecha_limite_retiro", nullable = false)
    private LocalDate fechaLimiteRetiro;

    @Column(name = "cantidad_bultos", nullable = false)
    @Builder.Default
    private Integer cantidadBultos = 0;

    @Column(name = "peso_bruto_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pesoBrutoKg = BigDecimal.ZERO;

    @Column(name = "ticket_pesaje_vgm", length = 50)
    private String ticketPesajeVgm;

    @Column(name = "precinto_conforme", nullable = false)
    @Builder.Default
    private Boolean precintoConforme = true;

    @Column(name = "autorizado_movimiento", nullable = false)
    @Builder.Default
    private Boolean autorizadoMovimiento = false;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "estado_almacen", nullable = false, length = 30)
    @Builder.Default
    private String estadoAlmacen = "EN_CUSTODIA";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

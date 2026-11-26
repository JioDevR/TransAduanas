package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.TipoVehiculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "unidad_transporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Integer idUnidad;

    @Column(name = "placa_tracto", nullable = false, unique = true, length = 15)
    private String placaTracto;

    @Column(name = "placa_carreta", length = 15)
    private String placaCarreta;

    @Column(name = "marca_modelo", nullable = false, length = 100)
    private String marcaModelo;

    @Column(name = "ano_fabricacion")
    private Integer anoFabricacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 50)
    @Builder.Default
    private TipoVehiculo tipoVehiculo = TipoVehiculo.TRACTO;

    @Column(name = "capacidad_carga_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal capacidadCargaKg = new BigDecimal("30000.00");

    @Column(name = "soat_vencimiento")
    private LocalDate soatVencimiento;

    @Column(name = "citv_vencimiento")
    private LocalDate citvVencimiento;

    @Column(name = "kilometraje_actual", nullable = false)
    @Builder.Default
    private Integer kilometrajeActual = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 20)
    @Builder.Default
    private EstadoOperativoFlota estadoOperativo = EstadoOperativoFlota.DISPONIBLE;

    @Column(name = "estado_activo", nullable = false)
    @Builder.Default
    private Boolean estadoActivo = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

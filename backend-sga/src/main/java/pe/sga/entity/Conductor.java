package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.CategoriaLicenciaMtcConverter;
import pe.sga.entity.enums.EstadoOperativoConductor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "conductor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conductor")
    private Integer idConductor;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "numero_documento", unique = true, length = 15)
    private String numeroDocumento;

    @Convert(converter = CategoriaLicenciaMtcConverter.class)
    @Column(name = "tipo_licencia", nullable = false, length = 10)
    @Builder.Default
    private CategoriaLicenciaMtc tipoLicencia = CategoriaLicenciaMtc.A_IIIC;

    @Column(name = "numero_licencia", nullable = false, unique = true, length = 20)
    private String numeroLicencia;

    @Column(name = "fecha_vencimiento_licencia")
    private LocalDate fechaVencimientoLicencia;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 20)
    @Builder.Default
    private EstadoOperativoConductor estadoOperativo = EstadoOperativoConductor.DISPONIBLE;

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

package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "numero_documento", length = 15)
    private String numeroDocumento;

    @Column(name = "tipo_licencia", nullable = false, length = 10)
    @Builder.Default
    private String tipoLicencia = "A-IIIc";

    @Column(name = "numero_licencia", nullable = false, unique = true, length = 20)
    private String numeroLicencia;

    @Column(name = "fecha_vencimiento_licencia")
    private LocalDate fechaVencimientoLicencia;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "estado_operativo", nullable = false, length = 20)
    @Builder.Default
    private String estadoOperativo = "DISPONIBLE";

    @Column(name = "estado_activo", nullable = false)
    @Builder.Default
    private Boolean estadoActivo = true;
}

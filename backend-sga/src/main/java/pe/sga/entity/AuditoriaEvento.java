package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_actor", nullable = false, length = 80)
    private String usuarioActor;

    @Column(name = "rol_actor", length = 50)
    private String rolActor;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", length = 60)
    private String entidad;

    @Column(name = "id_entidad", length = 50)
    private String idEntidad;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Builder.Default
    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado = "EXITO";

    @PrePersist
    protected void onCreate() {
        if (this.fecha == null) {
            this.fecha = LocalDateTime.now();
        }
        if (this.resultado == null || this.resultado.isBlank()) {
            this.resultado = "EXITO";
        }
    }
}

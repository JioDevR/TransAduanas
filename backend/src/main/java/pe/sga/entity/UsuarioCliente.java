package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_cliente")
    private Integer idUsuarioCliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo_electronico", nullable = false, length = 150)
    private String correoElectronico;

    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;

    @Column(name = "cargo_empresa", length = 100)
    private String cargoEmpresa;

    @Builder.Default
    @Column(name = "rol_portal", nullable = false, length = 50)
    private String rolPortal = "ADMIN_EMPRESA";

    @Builder.Default
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;

    @Builder.Default
    @Column(name = "debe_cambiar_password", nullable = false)
    private Boolean debeCambiarPassword = false;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + (apellidos != null ? " " + apellidos : "");
    }
}

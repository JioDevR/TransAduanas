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

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

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
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Métodos delegados a Cliente para normalización 3NF sin duplicación
    public String getNombreCompleto() {
        if (cliente == null) return username;
        return cliente.getContactoRepresentante() != null && !cliente.getContactoRepresentante().isBlank()
                ? cliente.getContactoRepresentante()
                : cliente.getRazonSocial();
    }

    public String getNombres() {
        return cliente != null ? cliente.getContactoRepresentante() : null;
    }

    public String getApellidos() {
        return "";
    }

    public String getCorreoElectronico() {
        return cliente != null ? cliente.getCorreoContacto() : null;
    }

    public String getTelefonoContacto() {
        return cliente != null ? cliente.getTelefonoContacto() : null;
    }

    public String getContactoRepresentante() {
        return cliente != null ? cliente.getContactoRepresentante() : null;
    }
}

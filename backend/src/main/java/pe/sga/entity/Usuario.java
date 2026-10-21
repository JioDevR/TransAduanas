package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sede")
    private Sede sede;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 100)
    private String correoInstitucional;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    @Builder.Default
    private String tipoDocumento = "DNI";

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "cargo_titulo", length = 100)
    private String cargoTitulo;

    @Column(name = "codigo_sunat", length = 50)
    private String codigoSunat;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Builder.Default
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;

    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + (apellidos != null ? " " + apellidos : "");
    }
}


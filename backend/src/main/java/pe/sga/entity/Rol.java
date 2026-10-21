package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.sga.entity.enums.RolUsuario;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private RolUsuario nombreRol;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Builder.Default
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;
}

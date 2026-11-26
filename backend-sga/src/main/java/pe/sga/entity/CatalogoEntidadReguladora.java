package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_entidad_reguladora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoEntidadReguladora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Integer idEntidad;

    @Column(name = "sigla", nullable = false, unique = true, length = 50)
    private String sigla;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

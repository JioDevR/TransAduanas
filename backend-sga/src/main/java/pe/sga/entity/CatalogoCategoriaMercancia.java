package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_categoria_mercancia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoCategoriaMercancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

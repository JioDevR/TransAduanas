package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_provincia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoProvincia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provincia")
    private Integer idProvincia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_departamento", nullable = false)
    private CatalogoDepartamento departamento;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_puerto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoPuerto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_puerto")
    private Integer idPuerto;

    @Column(name = "codigo_puerto", nullable = false, unique = true, length = 20)
    private String codigoPuerto;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "pais", nullable = false, length = 50)
    private String pais;

    @Column(name = "tipo", nullable = false, length = 30)
    @Builder.Default
    private String tipo = "MARITIMO";

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

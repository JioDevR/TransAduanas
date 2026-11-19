package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "catalogo_mercancia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoMercancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mercancia")
    private Integer idMercancia;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "categoria", nullable = false, length = 80)
    private String categoria;

    @Column(name = "requiere_permiso", nullable = false)
    @Builder.Default
    private Boolean requierePermiso = false;

    @Column(name = "entidad_reguladora", length = 80)
    private String entidadReguladora;

    @Column(name = "arancel_base_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal arancelBasePct = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

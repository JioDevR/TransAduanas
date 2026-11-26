package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_metodo_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoMetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private Integer idMetodoPago;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "aplica_cotizacion", nullable = false)
    @Builder.Default
    private Boolean aplicaCotizacion = true;

    @Column(name = "aplica_liquidacion", nullable = false)
    @Builder.Default
    private Boolean aplicaLiquidacion = true;

    @Column(name = "aplica_portal_b2b", nullable = false)
    @Builder.Default
    private Boolean aplicaPortalB2b = true;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

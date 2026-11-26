package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_cuenta_recaudadora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoCuentaRecaudadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer idCuenta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_banco", nullable = false)
    private CatalogoBanco banco;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "moneda", nullable = false, length = 5)
    @Builder.Default
    private String moneda = "USD";

    @Column(name = "numero_cuenta", nullable = false, length = 50)
    private String numeroCuenta;

    @Column(name = "cci", length = 50)
    private String cci;

    @Column(name = "aplica_portal_b2b", nullable = false)
    @Builder.Default
    private Boolean aplicaPortalB2b = true;

    @Column(name = "aplica_sga", nullable = false)
    @Builder.Default
    private Boolean aplicaSga = true;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}

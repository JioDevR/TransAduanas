package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sede")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Integer idSede;

    @Column(name = "codigo_sede", nullable = false, unique = true, length = 50)
    private String codigoSede;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 50)
    @Builder.Default
    private String tipo = "SEDE_CENTRAL";

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "distrito", nullable = false, length = 100)
    private String distrito;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "estado", nullable = false, length = 50)
    @Builder.Default
    private String estado = "OPERATIVO";
}

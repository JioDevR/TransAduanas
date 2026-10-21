package pe.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    @Builder.Default
    private String tipoDocumento = "RUC";

    @Column(name = "numero_documento", nullable = false, unique = true, length = 15)
    private String numeroDocumento;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "contacto_representante", length = 100)
    private String contactoRepresentante;

    @Column(name = "direccion_fiscal", length = 200)
    private String direccionFiscal;

    @Column(name = "correo_contacto", nullable = false, length = 100)
    private String correoContacto;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "es_prospecto", nullable = false)
    @Builder.Default
    private Boolean esProspecto = true;

    @Column(name = "estado_activo", nullable = false)
    @Builder.Default
    private Boolean estadoActivo = true;
}

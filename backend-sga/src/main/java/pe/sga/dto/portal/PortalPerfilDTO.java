package pe.sga.dto.portal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalPerfilDTO {
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String cargoTitulo;
    private String telefono;
    private Boolean debeCambiarPassword;

    // Datos de la empresa cliente
    private Integer idCliente;
    private String ruc;
    private String razonSocial;
    private String contactoRepresentante;
    private String direccionFiscal;
    private String correoContacto;
    private String telefonoContacto;
}

package pe.sga.dto.portal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalAuthResponseDTO {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Integer idUsuarioCliente;
    private String username;
    private String nombres;
    private String apellidos;
    private String correoElectronico;
    private String cargoEmpresa;
    private String rolPortal;
    private Integer idCliente;
    private String ruc;
    private String razonSocial;
    private Boolean debeCambiarPassword;
}

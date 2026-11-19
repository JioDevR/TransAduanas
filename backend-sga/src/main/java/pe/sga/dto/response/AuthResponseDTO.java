package pe.sga.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String rol;
    private Integer idSede;
    private String codigoSede;
    private String nombreSede;
}

package pe.sga.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Integer idUsuario;
    private Integer idRol;
    private String nombreRol;
    private Integer idSede;
    private String codigoSede;
    private String nombreSede;
    private String username;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String tipoDocumento;
    private String numeroDocumento;
    private String telefono;
    private String cargoTitulo;
    private String codigoSunat;
    private LocalDateTime ultimoAcceso;
    private Boolean estadoActivo;
}

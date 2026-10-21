package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.RolUsuario;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolResponseDTO {

    private Integer idRol;
    private RolUsuario nombreRol;
    private String descripcion;
    private Boolean estadoActivo;
}

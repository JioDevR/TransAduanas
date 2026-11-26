package pe.sga.dto.reporte;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPortalReporteItemDTO {

    private String username;
    private String empresa;
    private String ruc;
    private String representante;
    private String correo;
    private String estado;
}

package pe.sga.dto.reporte;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioReporteItemDTO {

    private String username;
    private String nombreCompleto;
    private String correo;
    private String numero;
    private String cargo;
    private String sede;
    private String estado;
}

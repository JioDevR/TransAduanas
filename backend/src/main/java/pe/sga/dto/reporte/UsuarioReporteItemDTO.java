package pe.sga.dto.reporte;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioReporteItemDTO {

    private Integer idUsuario;
    private String username;
    private String nombreCompleto;
    private String rol;
    private String cargoTitulo;
    private String sedeAgencia;
    private String estadoActivo;
}

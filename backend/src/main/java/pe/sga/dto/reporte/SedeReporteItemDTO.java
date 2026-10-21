package pe.sga.dto.reporte;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeReporteItemDTO {

    private Integer idSede;
    private String codigoSede;
    private String nombre;
    private String tipo;
    private String distrito;
    private String telefono;
    private String estado;
}
